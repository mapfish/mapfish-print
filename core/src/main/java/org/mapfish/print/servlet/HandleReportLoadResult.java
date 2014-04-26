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

package org.mapfish.print.servlet;

import org.mapfish.print.servlet.job.FailedPrintJob;
import org.mapfish.print.servlet.job.SuccessfulPrintJob;
import org.mapfish.print.servlet.job.loader.ReportLoader;

import java.io.IOException;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Called when a report is loaded to be sent to the user.
 *
 * @param <R> The return value
 * @author Jesse on 4/26/2014.
 */
public interface HandleReportLoadResult<R> {

    /**
     * Called if no loader can be found for loading the report.
     *
     * @param httpServletResponse response object
     * @param referenceId         report id
     */
    R unsupportedLoader(HttpServletResponse httpServletResponse, String referenceId);

    /**
     * Called when a print succeeded.
     *
     * @param successfulPrintResult the result
     * @param httpServletResponse   the http reponse
     * @param reportURI             the uri to the report
     * @param loader                the loader for loading the report.
     */
    R successfulPrint(SuccessfulPrintJob successfulPrintResult, HttpServletResponse httpServletResponse, URI reportURI,
                      ReportLoader loader) throws IOException, ServletException;

    /**
     * Called when a print job failed.
     *
     * @param failedPrintJob      the failed print job
     * @param httpServletResponse the object for writing response
     */
    R failedPrint(FailedPrintJob failedPrintJob, HttpServletResponse httpServletResponse);

    /**
     * Called when the print job has not yet completed.
     *
     * @param httpServletResponse the object for writing response
     * @param referenceId report id
     */
    R printJobPending(HttpServletResponse httpServletResponse, String referenceId);
}
