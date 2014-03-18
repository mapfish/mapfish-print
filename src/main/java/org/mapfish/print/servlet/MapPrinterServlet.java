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
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.servlet.job.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * The default servlet.
 * 
 * @author Jesse
 *
 */
public class MapPrinterServlet extends BaseMapServlet {
    private static final long serialVersionUID = -5038318057436063687L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);

    // can be /app/capabilities.json
    private static final String CAPABILITIES_URL = "/capabilities.json";
    private static final String CREATE_URL = "/create";
    private static final String STATUS_URL = "/status/";
    private static final String RESULT_URL = "/get/";

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


    @Override
	protected final void doGet(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        // do the routing in function of the actual URL
        String additionalPath = httpServletRequest.getPathInfo().trim();
        if (additionalPath.isEmpty()) {
            // handle an odd case where path info returns an empty string
            additionalPath = httpServletRequest.getServletPath();
        }
        if (additionalPath.equals(CAPABILITIES_URL)) {
            getInfo(null, httpServletResponse);
        } else if (additionalPath.endsWith(CAPABILITIES_URL)) {
            final String app = additionalPath.substring(1, additionalPath.length() - CAPABILITIES_URL.length());
            getInfo(app, httpServletResponse);
        } else if (additionalPath.startsWith(STATUS_URL)) {
            getStatus(additionalPath, httpServletResponse);
        } else if (additionalPath.startsWith(RESULT_URL)) {
            getFile(additionalPath, httpServletRequest, httpServletResponse);
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, HttpStatus.NOT_FOUND);
        }
    }

    private void getStatus(final String additionalPath, final HttpServletResponse httpServletResponse) {
        String ref = readReportReference(additionalPath);
        boolean done = this.jobManager.isDone(ref);

        PrintWriter writer = null;
        try {
            httpServletResponse.setContentType("application/json; charset=utf-8");
            writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_DONE).value(done);
                Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(ref);
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

    private String readReportReference(final String additionalPath) {
        int index = additionalPath.lastIndexOf("/");
        return additionalPath.substring(index + 1);
    }

    @Override
	protected final void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        final String additionalPath = httpServletRequest.getPathInfo();
        if (additionalPath.equals(CREATE_URL)) {
            queueJob(httpServletRequest, httpServletResponse);
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, HttpStatus.NOT_FOUND);
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
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    protected final void queueJob(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
        String spec;
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (httpServletRequest.getMethod().equalsIgnoreCase("POST")) {
            try {
                spec = getSpecFromPostBody(httpServletRequest);
            } catch (IOException e) {
                error(httpServletResponse, "Missing 'spec' in request body", HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }
        } else {
            spec = httpServletRequest.getParameter(JSON_SPEC);
        }
        if (spec == null) {
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Queue spec=" + spec);
        }

        PJsonObject specJson = MapPrinter.parseSpec(spec);

        String ref = UUID.randomUUID().toString();
        PrintWriter writer = null;
        try {
            PrintJob job = this.printJobFactory.create();
            
            job.setReferenceId(ref);
            job.setRequestData(specJson);
            job.setHeaders(getHeaders(httpServletRequest));

            this.jobManager.submit(job);

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
     * Get the json request data from the request object.
     * @param httpServletRequest the request object.
     */
    protected final String getSpecFromPostBody(final HttpServletRequest httpServletRequest) throws IOException {
        if (httpServletRequest.getParameter("spec") != null) {
            return httpServletRequest.getParameter("spec");
        }
        BufferedReader data = httpServletRequest.getReader();
        try {
            StringBuilder spec = new StringBuilder();
            String cur;
            while ((cur = data.readLine()) != null) {
                spec.append(cur).append("\n");
            }
            return spec.toString();
        } finally {
            if (data != null) {
                data.close();
            }
        }
    }

    /**
     * To get the PDF created previously.
     * 
     * @param additionalPath the path to the file.
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    protected final void getFile(final String additionalPath, final HttpServletRequest httpServletRequest, 
    		final HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        String ref = readReportReference(additionalPath);
        URI pdfURI = this.jobManager.getURI(ref);
        ReportLoader loader = null;
        for (ReportLoader reportLoader : this.reportLoaders) {
            if (reportLoader.accepts(pdfURI)) {
                loader = reportLoader;
                break;
            }
        }
        Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(ref);

        if (loader == null) {
            error(httpServletResponse, "Print with ref=" + ref + " unknown", HttpStatus.NOT_FOUND);
            return;
        }
        sendPdfFile(metadata.get(), httpServletResponse, loader, pdfURI,
                Boolean.parseBoolean(httpServletRequest.getParameter("inline")));
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param app the name of the "app" or in other words, a mapping to the configuration file for this request.
     * @param resp the response object
     */
    protected final void getInfo(final String app, final HttpServletResponse resp) throws ServletException,
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
     * @param metadata the client request data
     * @param httpServletResponse the response object
     * @param reportLoader the object used for loading the report
     * @param reportURI the uri of the report
     * @param inline whether or not to inline the content
     */
    protected final void sendPdfFile(final CompletedPrintJob metadata, final HttpServletResponse httpServletResponse,
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
