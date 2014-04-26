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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.servlet.job.CompletedPrintJob;
import org.mapfish.print.servlet.job.FailedPrintJob;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.PrintJobFactory;
import org.mapfish.print.servlet.job.SuccessfulPrintJob;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The default servlet.
 *
 * @author Jesse
 */
@Controller
public class MapPrinterServlet extends BaseMapServlet {
    private static final long serialVersionUID = -5038318057436063687L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);

    // can be /app/capabilities.json
    private static final String CAPABILITIES_URL = "/capabilities.json";
    private static final String CREATE_AND_GET_URL = "/buildreport";
    private static final String STATUS_URL = "/status";
    private static final String REPORT_URL = "/report";

    /* Registry keys */

    /**
     * The layout tag in the json job, status and metadata.
     */
    private static final String JSON_ERROR = "error";
    /**
     * The application ID which indicates the configuration file to load.
     */
    public static final String JSON_APP = "app";
    private static final String JSON_COUNT = "count";
    /**
     * The json property name of the property that contains the request spec.
     */
    public static final String JSON_SPEC = "spec";
    private static final String JSON_DONE = "done";
    private static final String JSON_TIME = "time";

    @Autowired
    private JobManager jobManager;
    @Autowired
    private List<ReportLoader> reportLoaders;
    @Autowired
    private MapPrinterFactory printerFactory;
    @Autowired
    private PrintJobFactory printJobFactory;
    @Autowired
    private ServletInfo servletInfo;
    private long maxCreateAndGetWaitTimeInSeconds;

    /**
     * Maximum time to wait for a createAndGet request to complete before returning an error.
     *
     * @param maxCreateAndGetWaitTimeInSeconds the maximum time in seconds to wait for a report to be generated.
     */
    public final void setMaxCreateAndGetWaitTimeInSeconds(final long maxCreateAndGetWaitTimeInSeconds) {
        this.maxCreateAndGetWaitTimeInSeconds = maxCreateAndGetWaitTimeInSeconds;
    }

    @RequestMapping(value = STATUS_URL + "/{referenceId}", method = RequestMethod.GET)
    private void getStatus(final String referenceId, final HttpServletResponse httpServletResponse) {
        boolean done = this.jobManager.isDone(referenceId);

        PrintWriter writer = null;
        try {
            httpServletResponse.setContentType("application/json; charset=utf-8");
            writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_DONE).value(done);
                Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(referenceId);
                if (metadata.isPresent() && metadata.get() instanceof FailedPrintJob) {
                    json.key(JSON_ERROR).value(((FailedPrintJob) metadata.get()).getError());
                }
                if (!done) {
                    json.key(JSON_COUNT).value(this.jobManager.getLastPrintCount());
                    json.key(JSON_TIME).value(this.jobManager.getAverageTimeSpentPrinting());
                }
            }
            json.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Read the headers from the request.
     *
     * @param httpServletRequest the request object
     */
    protected final HttpHeaders getHeaders(final HttpServletRequest httpServletRequest) {
        @SuppressWarnings("rawtypes")
        Enumeration headersName = httpServletRequest.getHeaderNames();
        HttpHeaders headers = new HttpHeaders();
        while (headersName.hasMoreElements()) {
            String name = headersName.nextElement().toString();
            ArrayList<String> headerValues = Lists.newArrayList();
            @SuppressWarnings("unchecked")
            Enumeration<String> e = httpServletRequest.getHeaders(name);
            while (e.hasMoreElements()) {
                headerValues.add(e.nextElement());
            }
            headers.put(name, headerValues);
        }
        return headers;
    }

    /**
     * add the print job to the job queue.
     *
     * @param requestData         a json formatted string with the request data required to perform the report generation.
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = REPORT_URL, method = RequestMethod.POST)
    public final void createReport(@RequestBody final String requestData, final HttpServletRequest httpServletRequest,
                                   final HttpServletResponse httpServletResponse) {

        String ref = createAndSubmitPrintJob(requestData, httpServletRequest, httpServletResponse);
        if (ref == null) {
            error(httpServletResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PrintWriter writer = null;
        try {
            httpServletResponse.setContentType("application/json; charset=utf-8");

            writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key("ref").value(ref);
            }
            json.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * add the print job to the job queue.
     *
     * @param requestData         a json formatted string with the request data required to perform the report generation.
     * @param inline whether or not to inline the content
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = CREATE_AND_GET_URL, method = RequestMethod.POST)
    public final void createReportAndGet(@RequestBody final String requestData,
                                         @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                         final HttpServletRequest httpServletRequest,
                                         final HttpServletResponse httpServletResponse) throws IOException, ServletException,
            InterruptedException {

        String ref = createAndSubmitPrintJob(requestData, httpServletRequest, httpServletResponse);
        if (ref == null) {
            error(httpServletResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        final HandleReportLoadResult<Boolean> handler = new HandleReportLoadResult<Boolean>() {

            @Override
            public Boolean unsupportedLoader(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
                return true;
            }

            @Override
            public Boolean successfulPrint(final SuccessfulPrintJob successfulPrintResult,
                                           final HttpServletResponse httpServletResponse,
                                           final URI reportURI, final ReportLoader loader) throws IOException, ServletException {
                sendPdfFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return true;
            }

            @Override
            public Boolean failedPrint(final FailedPrintJob failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return true;
            }

            @Override
            public Boolean printJobPending(final HttpServletResponse httpServletResponse, final String referenceId) {
                return false;
            }
        };


        final boolean[] isDone = new boolean[]{false};
        long startWaitTime = System.currentTimeMillis();
        final long maxWaitTimeInMillis = TimeUnit.SECONDS.toMillis(this.maxCreateAndGetWaitTimeInSeconds);
        while (!isDone[0] && System.currentTimeMillis() - startWaitTime < maxWaitTimeInMillis) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            isDone[0] = loadReport(ref, httpServletResponse, handler);
        }
    }

    private String createAndSubmitPrintJob(final String requestData, final HttpServletRequest httpServletRequest,
                                           final HttpServletResponse httpServletResponse) {
        if (requestData == null) {
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return null;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Report request data=" + requestData);
        }

        PJsonObject specJson = MapPrinter.parseSpec(requestData);

        String ref = UUID.randomUUID().toString() + "@" + this.servletInfo.getServletId();

        PrintJob job = this.printJobFactory.create();

        job.setReferenceId(ref);
        job.setRequestData(specJson);
        job.setHeaders(getHeaders(httpServletRequest));

        this.jobManager.submit(job);
        return ref;
    }

    /**
     * To get the PDF created previously.
     *
     * @param referenceId         the path to the file.
     * @param inline              whether or not to inline the
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = REPORT_URL + "/{referenceId}", method = RequestMethod.GET)
    public final void getReport(@PathVariable final String referenceId,
                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                final HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        loadReport(referenceId, httpServletResponse, new HandleReportLoadResult<Void>() {

            @Override
            public Void unsupportedLoader(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void successfulPrint(final SuccessfulPrintJob successfulPrintResult, final HttpServletResponse httpServletResponse,
                                        final URI reportURI, final ReportLoader loader) throws IOException, ServletException {
                sendPdfFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return null;
            }

            @Override
            public Void failedPrint(final FailedPrintJob failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return null;
            }

            @Override
            public Void printJobPending(final HttpServletResponse httpServletResponse, final String referenceId) {
                return null;
            }
        });
    }

    private <R> R loadReport(final String referenceId, final HttpServletResponse httpServletResponse,
                             final HandleReportLoadResult<R> handler) throws IOException, ServletException {
        URI pdfURI = this.jobManager.getURI(referenceId);
        ReportLoader loader = null;
        for (ReportLoader reportLoader : this.reportLoaders) {
            if (reportLoader.accepts(pdfURI)) {
                loader = reportLoader;
                break;
            }
        }
        Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(referenceId);

        if (loader == null) {
            return handler.unsupportedLoader(httpServletResponse, referenceId);
        } else if (!metadata.isPresent()) {
            return handler.printJobPending(httpServletResponse, referenceId);
        } else if (metadata.get() instanceof SuccessfulPrintJob) {
            SuccessfulPrintJob succ = (SuccessfulPrintJob) metadata.get();
            return handler.successfulPrint(succ, httpServletResponse, pdfURI, loader);
        } else if (metadata.get() instanceof FailedPrintJob) {
            FailedPrintJob failedPrintJob = (FailedPrintJob) metadata.get();
            return handler.failedPrint(failedPrintJob, httpServletResponse);
        } else {
            throw new ServletException("Unexpected state");
        }

    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param resp the response object
     */
    @RequestMapping(value = CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getInfo(final HttpServletResponse resp) throws ServletException,
            IOException {
        getInfo(null, resp);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param app  the name of the "app" or in other words, a mapping to the configuration file for this request.
     * @param resp the response object
     */
    @RequestMapping(value = "/{appId}" + CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getInfo(final String app, final HttpServletResponse resp) throws ServletException,
            IOException {
        MapPrinter printer = this.printerFactory.create(app);
        resp.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = resp.getWriter();

        try {
            JSONWriter json = new JSONWriter(writer);
            try {
                json.object();
                {
                    printer.printClientConfig(json);

                    if (app != null) {
                        json.key(JSON_APP).value(app);
                    }
                }
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Copy the PDF into the output stream.
     *
     * @param metadata            the client request data
     * @param httpServletResponse the response object
     * @param reportLoader        the object used for loading the report
     * @param reportURI           the uri of the report
     * @param inline              whether or not to inline the content
     */
    protected final void sendPdfFile(final SuccessfulPrintJob metadata, final HttpServletResponse httpServletResponse,
                                     final ReportLoader reportLoader, final URI reportURI, final boolean inline)
            throws IOException, ServletException {

        final OutputStream response = httpServletResponse.getOutputStream();
        try {
            httpServletResponse.setContentType("application/pdf");
            if (!inline) {
                final String fileName = metadata.getFileName();
                httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName);
            }
            reportLoader.loadReport(reportURI, response);
        } finally {
            response.close();
        }
    }
}
