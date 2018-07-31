package org.mapfish.print.servlet;

import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.loader.ReportLoader;

import java.io.IOException;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Called when a report is loaded to be sent to the user.
 *
 * @param <R> The return value
 */
public interface HandleReportLoadResult<R> {

    /**
     * Called if the report reference is unknown.
     *
     * @param httpServletResponse response object
     * @param referenceId report id
     */
    R unknownReference(HttpServletResponse httpServletResponse, String referenceId);

    /**
     * Called if no loader can be found for loading the report.
     *
     * @param httpServletResponse response object
     * @param referenceId report id
     */
    R unsupportedLoader(HttpServletResponse httpServletResponse, String referenceId);

    /**
     * Called when a print succeeded.
     *
     * @param successfulPrintResult the result
     * @param httpServletResponse the http reponse
     * @param reportURI the uri to the report
     * @param loader the loader for loading the report.
     */
    R successfulPrint(
            PrintJobStatus successfulPrintResult, HttpServletResponse httpServletResponse, URI reportURI,
            ReportLoader loader) throws IOException, ServletException;

    /**
     * Called when a print job failed.
     *
     * @param failedPrintJob the failed print job
     * @param httpServletResponse the object for writing response
     */
    R failedPrint(PrintJobStatus failedPrintJob, HttpServletResponse httpServletResponse);

    /**
     * Called when the print job has not yet completed.
     *
     * @param httpServletResponse the object for writing response
     * @param referenceId report id
     */
    R printJobPending(HttpServletResponse httpServletResponse, String referenceId);
}
