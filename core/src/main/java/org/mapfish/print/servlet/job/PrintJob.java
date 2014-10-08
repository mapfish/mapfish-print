/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AndAccessAssertion;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * The information for printing a report.
 *
 * @author Jesse
 */
public abstract class PrintJob implements Callable<PrintJobStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintJob.class);

    private String referenceId;
    private PJsonObject requestData;
    private AccessAssertion access;

    @Autowired
    private MapPrinterFactory mapPrinterFactory;
    @Autowired
    private MetricRegistry metricRegistry;
    @Autowired
    private ApplicationContext applicationContext;

    private SecurityContext securityContext;

    /**
     * Get the reference id of the job so it can be looked up again later.
     */
    public final String getReferenceId() {
        return this.referenceId;
    }

    /**
     * Set the reference id of the job so it can be looked up again later.
     *
     * @param referenceId the referenceId
     */
    public final void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Set the data from the client making the request.
     *
     * @param requestData the json data
     */
    public final void setRequestData(final PJsonObject requestData) {
        this.requestData = requestData;
    }

    /**
     * Open an OutputStream and execute the function using the OutputStream.
     *
     * @param function the function to execute
     * @return the
     */
    protected abstract URI withOpenOutputStream(PrintAction function) throws Throwable;

    @Override
    public final PrintJobStatus call() throws Exception {
        SecurityContextHolder.setContext(this.securityContext);
        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + " call()").time();
        PJsonObject spec = null;
        MapPrinter mapPrinter = null;
        try {
            spec = PrintJob.this.requestData;
            mapPrinter = PrintJob.this.mapPrinterFactory.create(getAppId());
            final MapPrinter finalMapPrinter = mapPrinter;
            URI reportURI = withOpenOutputStream(new PrintAction() {
                @Override
                public void run(final OutputStream outputStream) throws Throwable {
                    finalMapPrinter.print(PrintJob.this.requestData, outputStream);
                }
            });

            this.metricRegistry.counter(getClass().getName() + "success").inc();
            LOGGER.debug("Successfully completed print job" + this.referenceId + "\n" + this.requestData);
            String fileName = getFileName(mapPrinter, spec);

            final OutputFormat outputFormat = mapPrinter.getOutputFormat(spec);
            String mimeType = outputFormat.getContentType();
            String fileExtension = outputFormat.getFileSuffix();

            return new SuccessfulPrintJob(this.referenceId, reportURI, getAppId(), new Date(), fileName, mimeType,
                    fileExtension, this.access);
        } catch (Throwable e) {
            String canceledText = "";
            if (Thread.currentThread().isInterrupted()) {
                canceledText = "(canceled) ";
            }
            LOGGER.info("Error executing print job " + canceledText + this.referenceId + "\n" + this.requestData, e);
            this.metricRegistry.counter(getClass().getName() + "failure").inc();
            String fileName = "unknownFileName";
            if (spec != null) {
                fileName = getFileName(mapPrinter, spec);
            }
            final Throwable rootCause = getRootCause(e);
            return new FailedPrintJob(this.referenceId, getAppId(), new Date(), fileName, rootCause.toString(), this.access);
        } finally {
            final long stop = TimeUnit.MILLISECONDS.convert(timer.stop(), TimeUnit.NANOSECONDS);
            LOGGER.debug("Print Job " + PrintJob.this.referenceId + " completed in " + stop + "ms");
        }
    }

    /**
     * Because exceptions might get re-thrown several times, an error message like
     * "java.util.concurrent.ExecutionException: java.lang.IllegalArgumentException: java.lang.IllegalArgumentException: ..."
     * might get created. To avoid this, this method finds the root cause, so that only a message like
     * "java.lang.IllegalArgumentException: ..." is shown.
     */
    private Throwable getRootCause(final Throwable e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    protected final String getAppId() {
        return PrintJob.this.requestData.optString(
                MapPrinterServlet.JSON_APP,
                ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY);
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

    public final AccessAssertion getAccess() {
        return this.access;
    }

    /**
     * Configure the access permissions required to access this print job.
     *
     * @param template the containing print template which should have sufficient information to configure the access.
     */
    public final void configureAccess(final Template template) {
        final Configuration configuration = template.getConfiguration();

        AndAccessAssertion accessAssertion = this.applicationContext.getBean(AndAccessAssertion.class);
        accessAssertion.setPredicates(configuration.getAccessAssertion(), template.getAccessAssertion());
        this.access = accessAssertion;
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
        void run(OutputStream outputStream) throws Throwable;
    }
}
