package org.mapfish.print.servlet.job;

import org.mapfish.print.config.access.AccessAssertion;

import java.util.Date;
import java.util.concurrent.Future;

/**
 * Encapsulates a job that has been submitted to the JobManager.
 */
public class SubmittedPrintJob {
    private final String reportRef;
    private final String appId;
    private final Future<PrintJobStatus> reportFuture;
    private final long startTime;
    private final AccessAssertion accessAssertion;

    /**
     * Constructor.
     *
     * @param reportFuture    the future for checking if the report is done and for getting the uri
     * @param reportRef       the unique ID for the report
     * @param appId           the app id
     * @param accessAssertion the an access control object for downloading this report.  Typically this is combined access of the
     *                        template and the configuration.
     */
    public SubmittedPrintJob(final Future<PrintJobStatus> reportFuture, final String reportRef, final String appId,
                             final AccessAssertion accessAssertion) {
        this.startTime = new Date().getTime();
        this.reportFuture = reportFuture;
        this.reportRef = reportRef;
        this.appId = appId;
        this.accessAssertion = accessAssertion;
    }

    /**
     * Get the unique ID for the report.
     */
    public final String getReportRef() {
        return this.reportRef;
    }
    
    /**
     * Get the app ID for the report.
     */
    public final String getAppId() {
        return this.appId;
    }

    /**
     * Get the future for checking if the report is done and for getting the uri.
     */
    public final Future<PrintJobStatus> getReportFuture() {
        return this.reportFuture;
    }

    /**
     * @return The time in milliseconds since the start.
     */
    public final long getTimeSinceStart() {
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * @return The start date.
     */
    public final Date getStartDate() {
        return new Date(this.startTime);
    }

    public final AccessAssertion getAccessAssertion() {
        return this.accessAssertion;
    }
}
