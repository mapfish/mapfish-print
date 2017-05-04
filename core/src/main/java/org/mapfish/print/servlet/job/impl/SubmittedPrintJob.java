package org.mapfish.print.servlet.job.impl;

import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;

import java.util.concurrent.Future;

/**
 * Encapsulates a job that has been submitted to the JobManager.
 */
public class SubmittedPrintJob {

    private final PrintJobEntry entry;
    private final Future<PrintJobResult> reportFuture;

    /**
     * Constructor.
     *
     * @param reportFuture the future for checking if the report is done and for getting the uri
     * @param entry the print job entry.
     */
    public SubmittedPrintJob(final Future<PrintJobResult> reportFuture, final PrintJobEntry entry) {
        this.reportFuture = reportFuture;
        this.entry = entry;
    }

    /**
     * Get the future for checking if the report is done and for getting the uri.
     */
    public final Future<PrintJobResult> getReportFuture() {
        return this.reportFuture;
    }

    public final PrintJobEntry getEntry() {
        return this.entry;
    }

}
