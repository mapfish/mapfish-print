package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.processor.Processor;
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
import javax.annotation.Nonnull;
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
    @Autowired
    private Accounting accounting;

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

    /**
     * Open an OutputStream and execute the function using the OutputStream.
     *
     * @param function the function to execute
     * @return the URI and the file size
     */
    protected abstract PrintResult withOpenOutputStream(PrintAction function) throws Exception;

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
    protected PrintJobResult createResult(
            final URI reportURI, final String fileName, final String fileExtension,
            final String mimeType, final String referenceId) {
        //CHECKSTYLE:ON
        return new PrintJobResultImpl(reportURI, fileName, fileExtension, mimeType, referenceId);
    }

    @Override
    public final PrintJobResult call() throws Exception {
        SecurityContextHolder.setContext(this.securityContext);
        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + ".call").time();
        MDC.put(Processor.MDC_JOB_ID_KEY, this.entry.getReferenceId());
        LOGGER.info("Starting print job {}", this.entry.getReferenceId());
        final MapPrinter mapPrinter = PrintJob.this.mapPrinterFactory.create(this.entry.getAppId());
        final Accounting.JobTracker jobTracker =
                this.accounting.startJob(this.entry, mapPrinter.getConfiguration());
        try {
            final PJsonObject spec = this.entry.getRequestData();
            PrintResult report = withOpenOutputStream(new PrintAction() {
                @Override
                public Processor.ExecutionContext run(final OutputStream outputStream) throws Exception {
                    return mapPrinter.print(PrintJob.this.entry.getReferenceId(),
                                            PrintJob.this.entry.getRequestData(), outputStream);
                }
            });

            this.metricRegistry.counter(getClass().getName() + ".success").inc();
            jobTracker.onJobSuccess(report);
            LOGGER.info("Successfully completed print job {}", this.entry.getReferenceId());
            LOGGER.debug("Job {}\n{}", this.entry.getReferenceId(), this.entry.getRequestData());
            String fileName = getFileName(mapPrinter, spec);

            String mimeType = null;
            String fileExtension = null;
            if (mapPrinter != null) { //can only happen in test
                final OutputFormat outputFormat = mapPrinter.getOutputFormat(spec);
                mimeType = outputFormat.getContentType();
                fileExtension = outputFormat.getFileSuffix();
            }
            return createResult(report.uri, fileName, fileExtension, mimeType, this.entry.getReferenceId());
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
    public class PrintResult {
        /**
         * The URI to get the result.
         */
        @Nonnull
        public final URI uri;

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
         * @param uri the
         * @param fileSize the
         * @param executionContext the
         */
        public PrintResult(
                final URI uri, final long fileSize,
                final Processor.ExecutionContext executionContext) {
            this.uri = uri;
            this.fileSize = fileSize;
            this.executionContext = executionContext;
        }
    }
}
