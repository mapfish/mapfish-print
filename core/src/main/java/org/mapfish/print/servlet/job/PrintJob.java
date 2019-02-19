package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.SmtpConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.processor.ExecutionStats;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.servlet.job.impl.PrintJobEntryImpl;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * The information for printing a report.
 */
public abstract class PrintJob implements Callable<PrintJobResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintJob.class);
    private PrintJobEntry entry;

    @Autowired
    private MapPrinterFactory mapPrinterFactory;
    @Autowired
    private MetricRegistry metricRegistry;
    @Autowired
    private Accounting accounting;
    @Autowired
    private WorkingDirectories workingDirectories;

    private SecurityContext securityContext;

    /**
     * Read filename from spec.
     */
    private static String getFileName(@Nullable final MapPrinter mapPrinter, final PJsonObject spec) {
        String fileName = spec.optString(Constants.OUTPUT_FILENAME_KEY);
        if (fileName != null) {
            return fileName;
        }

        if (mapPrinter != null) {
            final Configuration config = mapPrinter.getConfiguration();
            final String templateName = spec.getString(Constants.JSON_LAYOUT_KEY);

            final Template template = config.getTemplate(templateName);

            if (template.getOutputFilename() != null) {
                return template.getOutputFilename();
            }

            if (config.getOutputFilename() != null) {
                return config.getOutputFilename();
            }
        }
        return "mapfish-print-report";
    }

    public final PrintJobEntry getEntry() {
        return this.entry;
    }

    public final void setEntry(final PrintJobEntry entry) {
        this.entry = entry;
    }

    protected File getReportFile() {
        return new File(workingDirectories.getReports(), getEntry().getReferenceId());
    }

    /**
     * Open an OutputStream and execute the function using the OutputStream.
     *
     * @param function the function to execute
     * @return the URI and the file size
     */
    protected PrintResult withOpenOutputStream(final PrintAction function) throws Exception {
        final File reportFile = getReportFile();
        final Processor.ExecutionContext executionContext;
        try (FileOutputStream out = new FileOutputStream(reportFile);
             BufferedOutputStream bout = new BufferedOutputStream(out)) {
            executionContext = function.run(bout);
        }
        return new PrintResult(reportFile.length(), executionContext);
    }


    /**
     * Create Print Job Result.
     *
     * @param fileName the file name
     * @param fileExtension the file extension
     * @param mimeType the mime type
     * @return the job result
     */
    protected abstract PrintJobResult createResult(
            String fileName, String fileExtension,
            String mimeType) throws URISyntaxException, IOException;

    @Override
    public final PrintJobResult call() throws Exception {
        SecurityContextHolder.setContext(this.securityContext);
        final Timer.Context timer = this.metricRegistry.timer(getClass().getName() + ".call").time();
        MDC.put(Processor.MDC_JOB_ID_KEY, this.entry.getReferenceId());
        LOGGER.info("Starting print job {}", this.entry.getReferenceId());
        final MapPrinter mapPrinter = PrintJob.this.mapPrinterFactory.create(this.entry.getAppId());
        final Accounting.JobTracker jobTracker =
                this.accounting.startJob(this.entry, mapPrinter.getConfiguration());
        try {
            final PJsonObject spec = this.entry.getRequestData();
            final PrintResult report = withOpenOutputStream(
                    outputStream -> mapPrinter.print(entry.getReferenceId(), entry.getRequestData(),
                                                     outputStream));

            this.metricRegistry.counter(getClass().getName() + ".success").inc();
            LOGGER.info("Successfully completed print job {}", this.entry.getReferenceId());
            LOGGER.debug("Job {}\n{}", this.entry.getReferenceId(), this.entry.getRequestData());
            final String fileName = getFileName(mapPrinter, spec);

            final OutputFormat outputFormat = mapPrinter.getOutputFormat(spec);
            final String mimeType = outputFormat.getContentType();
            final String fileExtension = outputFormat.getFileSuffix();
            final boolean sent =
                    maybeSendResult(mapPrinter.getConfiguration(), fileName, fileExtension, mimeType,
                                    report.executionContext.getStats());
            jobTracker.onJobSuccess(report);
            return sent ?
                    null :
                    createResult(fileName, fileExtension, mimeType);
        } catch (Exception e) {
            String canceledText = "";
            if (Thread.currentThread().isInterrupted()) {
                canceledText = "(canceled) ";
                this.metricRegistry.counter(getClass().getName() + ".canceled").inc();
                jobTracker.onJobCancel();
            } else {
                this.metricRegistry.counter(getClass().getName() + ".error").inc();
                jobTracker.onJobError();
            }
            deleteReport();
            maybeSendError(mapPrinter.getConfiguration(), e);
            LOGGER.warn("Error executing print job {} {}\n{}",
                        this.entry.getRequestData(), canceledText, this.entry.getReferenceId(), e);
            throw e;
        } finally {
            final long totalTimeMS = System.currentTimeMillis() - entry.getStartTime();
            final long computationTimeMs = TimeUnit.MILLISECONDS.convert(timer.stop(), TimeUnit.NANOSECONDS);
            this.metricRegistry.timer(getClass().getName() + ".total")
                    .update(totalTimeMS, TimeUnit.MILLISECONDS);
            this.metricRegistry.timer(getClass().getName() + ".wait")
                    .update(totalTimeMS - computationTimeMs, TimeUnit.MILLISECONDS);
            LOGGER.debug("Print Job {} completed in {}ms", this.entry.getReferenceId(), computationTimeMs);
            MDC.remove(Processor.MDC_JOB_ID_KEY);
        }
    }

    private void maybeSendError(final Configuration configuration, final Exception e) {
        final PJsonObject requestData = entry.getRequestData();
        final SmtpConfig smtp = configuration.getSmtp();
        final PJsonObject requestSmtp = requestData.optJSONObject("smtp");
        if (smtp == null || requestSmtp == null) {
            return;
        }
        try {
            sendErrorEmail(smtp, requestSmtp, e);
        } catch (MessagingException sendException) {
            LOGGER.warn("Error sending error email", sendException);
        }
    }

    private void sendErrorEmail(
            final SmtpConfig config, final PJsonObject request, final Exception e)
            throws MessagingException {
        final String to = request.getString("to");
        final InternetAddress[] recipients = InternetAddress.parse(to);
        final Message message = createMessage(config, recipients);
        message.setSubject(request.optString("errorSubject", config.getErrorSubject()));

        final String msg = request.optString("errorBody", config.getErrorBody()).
                replace("{message}", ExceptionUtils.getRootCause(e).toString());
        final MimeBodyPart html = new MimeBodyPart();
        html.setContent(msg, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(html);

        message.setContent(multipart);

        LOGGER.info("Emailing error to {}", to);
        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + ".email.success").time();
        Transport.send(message);
        timer.stop();
    }

    private Message createMessage(final SmtpConfig config, final InternetAddress[] recipients)
            throws MessagingException {
        final Session session = createEmailSession(config);
        final Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getFromAddress()));
        message.setRecipients(
                Message.RecipientType.TO, recipients);
        return message;
    }

    private boolean maybeSendResult(
            final Configuration configuration, final String fileName, final String fileExtension,
            final String mimeType, final ExecutionStats stats)
            throws IOException, MessagingException {
        final PJsonObject requestData = entry.getRequestData();
        final SmtpConfig smtp = configuration.getSmtp();
        final PJsonObject requestSmtp = requestData.optJSONObject("smtp");
        if (smtp == null || requestSmtp == null) {
            return false;
        }
        sendEmail(smtp, requestSmtp, fileName, fileExtension, mimeType, stats);
        deleteReport();
        return true;
    }

    private void sendEmail(
            final SmtpConfig config, final PJsonObject request, final String fileName,
            final String fileExtension, final String mimeType,
            final ExecutionStats stats) throws MessagingException, IOException {
        final String to = request.getString("to");
        final InternetAddress[] recipients = InternetAddress.parse(to);
        final Message message = createMessage(config, recipients);
        message.setSubject(request.optString("subject", config.getSubject()));

        String msg = request.optString("body", config.getBody());
        if (config.getStorage() != null) {
            final Timer.Context saveTimer =
                    this.metricRegistry.timer(config.getStorage().getClass().getName()).time();
            final URL url = config.getStorage().save(
                    this.entry.getReferenceId(), fileName, fileExtension, mimeType, getReportFile());
            saveTimer.stop();
            msg = msg.replace("{url}", url.toString());
        }
        final MimeBodyPart html = new MimeBodyPart();
        html.setContent(msg, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(html);

        if (config.getStorage() == null) {
            final MimeBodyPart attachement = new MimeBodyPart();
            attachement.attachFile(getReportFile(), mimeType, null);
            attachement.setFileName(fileName + "." + fileExtension);
            multipart.addBodyPart(attachement);
        }

        message.setContent(multipart);

        LOGGER.info("Emailing result to {}", to);
        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + ".email.error").time();
        Transport.send(message);
        timer.stop();
        stats.addEmailStats(recipients, config.getStorage() != null);
    }

    /**
     * Delete the report (used if the report is sent by email).
     */
    protected void deleteReport() {
        final File reportFile = getReportFile();
        if (!reportFile.exists()) {
            return;
        }
        if (!reportFile.delete()) {
            LOGGER.warn("Failed deleting the temporary print report");
        }
    }

    private Session createEmailSession(final SmtpConfig config) {
        Properties prop = new Properties();
        prop.put("mail.smtp.starttls.enable", config.isStarttls());
        prop.put("mail.smtp.ssl.enable", config.isSsl());
        prop.put("mail.smtp.host", config.getHost());
        prop.put("mail.smtp.port", Integer.toString(config.getPort()));
        // prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");
        final Session session;
        if (config.getUsername() != null) {
            prop.put("mail.smtp.auth", true);
            session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });
        } else {
            session = Session.getInstance(prop);
        }
        return session;
    }

    /**
     * The security context that contains the information about the user that made the request.  This must be
     * set on {@link org.springframework.security.core.context.SecurityContextHolder} when the thread starts
     * executing.
     *
     * @param securityContext the conext object
     */
    public final void setSecurityContext(final SecurityContext securityContext) {
        this.securityContext = SecurityContextHolder.createEmptyContext();
        this.securityContext.setAuthentication(securityContext.getAuthentication());
    }

    final void initForTesting(final ApplicationContext context) {
        this.metricRegistry = context.getBean(MetricRegistry.class);
        this.mapPrinterFactory = new MapPrinterFactory() {
            @Override
            public MapPrinter create(final String app) {
                return null;
            }

            @Override
            public Set<String> getAppIds() {
                return null;
            }
        };
        this.entry = new PrintJobEntryImpl();
    }

    /**
     * Interface encapsulating the code to run with the open output stream.
     */
    protected interface PrintAction {
        /**
         * Execute the action.
         *
         * @param outputStream the output stream to write the report to.
         */
        Processor.ExecutionContext run(OutputStream outputStream) throws Exception;
    }

    /**
     * Holds the info that goes with the result of a print.
     */
    public static class PrintResult {
        /**
         * The result size in bytes.
         */
        public final long fileSize;

        /**
         * The execution context used during the computation.
         */
        @Nonnull
        public final Processor.ExecutionContext executionContext;

        /**
         * Constructor.
         *
         * @param fileSize the
         * @param executionContext the
         */
        public PrintResult(final long fileSize, final Processor.ExecutionContext executionContext) {
            this.fileSize = fileSize;
            this.executionContext = executionContext;
        }
    }
}
