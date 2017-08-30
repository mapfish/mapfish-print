package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.servlet.NoSuchAppException;
import org.mapfish.print.servlet.job.impl.PrintJobEntryImpl;
import org.mapfish.print.servlet.job.impl.PrintJobResultImpl;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.OutputStream;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

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

    private SecurityContext securityContext;

    public final PrintJobEntry getEntry() {
        return this.entry;
    }

    public final void setEntry(final PrintJobEntry entry) {
        this.entry = entry;
    }

    /**
     * Open an OutputStream and execute the function using the OutputStream.
     *
     * @param function the function to execute
     * @return the
     */
    protected abstract URI withOpenOutputStream(PrintAction function) throws Exception;

    /**
     * Create Print Job Result.
     *
     * @param reportURI the report URI
     * @param fileName the file name
     * @param fileExtension the file extension
     * @param mimeType the mime type
     * @return the job result
     */
    //CHECKSTYLE:OFF
    protected PrintJobResult createResult(final URI reportURI, final String fileName, final String fileExtension,
            final String mimeType, final String referenceId) {
    //CHECKSTYLE:ON
        return new PrintJobResultImpl(reportURI, fileName, fileExtension, mimeType, referenceId);
    }

    @Override
    public final PrintJobResult call() throws Exception {
        SecurityContextHolder.setContext(this.securityContext);
        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + " call()").time();
        PJsonObject spec = null;
        MapPrinter mapPrinter = null;
        try {
            MDC.put("job_id", this.entry.getReferenceId());
            LOGGER.info("Starting print job " + this.entry.getReferenceId());
            spec = this.entry.getRequestData();
            mapPrinter = PrintJob.this.mapPrinterFactory.create(this.entry.getAppId());
            final MapPrinter finalMapPrinter = mapPrinter;
            URI reportURI = withOpenOutputStream(new PrintAction() {
                @Override
                public void run(final OutputStream outputStream) throws Exception {
                    finalMapPrinter.print(PrintJob.this.entry.getReferenceId(),
                            PrintJob.this.entry.getRequestData(), outputStream);
                }
            });

            this.metricRegistry.counter(getClass().getName() + "success").inc();
            LOGGER.info("Successfully completed print job " + this.entry.getReferenceId());
            LOGGER.debug("Job " + this.entry.getReferenceId() + "\n" + this.entry.getRequestData());
            String fileName = getFileName(mapPrinter, spec);

            String mimeType = null;
            String fileExtension = null;
            if (mapPrinter != null) { //can only happen in test
                final OutputFormat outputFormat = mapPrinter.getOutputFormat(spec);
                mimeType = outputFormat.getContentType();
                fileExtension = outputFormat.getFileSuffix();
            }
            return createResult(reportURI, fileName, fileExtension, mimeType, this.entry.getReferenceId());
        } catch (Exception e) {
            String canceledText = "";
            if (Thread.currentThread().isInterrupted()) {
                canceledText = "(canceled) ";
            }
            LOGGER.info("Error executing print job " + canceledText + this.entry.getReferenceId() + "\n" + this.entry.getRequestData(), e);
            this.metricRegistry.counter(getClass().getName() + "failure").inc();
            throw e;
        } finally {
            final long stop = TimeUnit.MILLISECONDS.convert(timer.stop(), TimeUnit.NANOSECONDS);
            LOGGER.debug("Print Job " + this.entry.getReferenceId() + " completed in " + stop + "ms");
        }
    }

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

    /**
     * The security context that contains the information about the user that made the request.  This must be
     * set on {@link org.springframework.security.core.context.SecurityContextHolder} when the thread starts executing.
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
            public MapPrinter create(final String app) throws NoSuchAppException {
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
        void run(OutputStream outputStream) throws Exception;
    }
}
