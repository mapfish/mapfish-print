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
import com.google.common.io.Files;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.servlet.job.CompletedPrintJob;
import org.mapfish.print.servlet.job.FailedPrintJob;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.SuccessfulPrintJob;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

/**
 * The default servlet.
 *
 * @author Jesse
 * CSOFF: RedundantThrowsCheck
 */
@Controller
public class MapPrinterServlet extends BaseMapServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPrinterServlet.class);

    private static final String CAPABILITIES_URL = "/capabilities.json";
    private static final String LIST_APPS_URL = "/apps.json";
    private static final String EXAMPLE_REQUEST_URL = "/exampleRequest.json";
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
    /**
     * The number of print jobs done by the cluster (or this server if count is not shared through-out cluster).
     * <p/>
     * Part of the {@link #getStatus(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} response
     */
    public static final String JSON_COUNT = "count";
    /**
     * The json property name of the property that contains the request spec.
     */
    public static final String JSON_SPEC = "spec";
    /**
     * If the job is done (value is true) or not (value is false).
     * <p/>
     * Part of the {@link #getStatus(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} response
     */
    public static final String JSON_DONE = "done";
    /**
     * The time taken for the job to complete.
     * <p/>
     * Part of the {@link #getStatus(String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} response
     */
    public static final String JSON_TIME = "time";
    /**
     * The key containing the print job reference ID in the create report response.
     */
    public static final String JSON_PRINT_JOB_REF = "ref";
    /**
     * The json key in the create report response containing a link to get the status of the print job.
     */
    public static final String JSON_STATUS_LINK = "statusURL";
    /**
     * The json key in the create report and status responses containing a link to download the report.
     */
    public static final String JSON_DOWNLOAD_LINK = "downloadURL";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\S+)}");
    /**
     * The JSON key in the request spec that contains the outputFormat.  This value will be put into
     * the spec by the servlet.  there is not need for the post to do this.
     */
    public static final String JSON_OUTPUT_FORMAT = "outputFormat";
    private static final int JSON_INDENT_FACTOR = 4;

    @Autowired
    private JobManager jobManager;
    @Autowired
    private List<ReportLoader> reportLoaders;
    @Autowired
    private MapPrinterFactory printerFactory;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private ServletInfo servletInfo;

    private long maxCreateAndGetWaitTimeInSeconds;

    /**
     * Get a status report on a job.  Returns the following json:
     * <p/>
     * <pre><code>
     *  {"time":0,"count":0,"done":false}
     * </code></pre>
     *
     * @param referenceId    the job reference
     * @param statusRequest  the request object
     * @param statusResponse the response object
     */
    @RequestMapping(value = STATUS_URL + "/{referenceId:\\S+}.json", method = RequestMethod.GET)
    public final void getStatus(@PathVariable final String referenceId, final HttpServletRequest statusRequest,
                                final HttpServletResponse statusResponse) {
        boolean done = this.jobManager.isDone(referenceId);

        PrintWriter writer = null;
        try {
            statusResponse.setContentType("application/json; charset=utf-8");
            writer = statusResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_DONE).value(done);
                Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(referenceId);
                if (metadata.isPresent() && metadata.get() instanceof FailedPrintJob) {
                    json.key(JSON_ERROR).value(((FailedPrintJob) metadata.get()).getError());
                }
                json.key(JSON_COUNT).value(this.jobManager.getLastPrintCount());
                json.key(JSON_TIME).value(this.jobManager.getAverageTimeSpentPrinting());

                addDownloadLinkToJson(statusRequest, referenceId, json);
            }
            json.endObject();
        } catch (JSONException e) {
            LOGGER.error("Error obtaining status", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("Error obtaining status", e);
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * add the print job to the job queue.
     *
     * @param appId                the id of the app to get the request for.
     * @param format               the format of the returned report
     * @param requestData          a json formatted string with the request data required to perform the report generation.
     * @param createReportRequest  the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(@PathVariable final String appId,
                                   @PathVariable final String format,
                                   @RequestBody final String requestData,
                                   final HttpServletRequest createReportRequest,
                                   final HttpServletResponse createReportResponse) throws JSONException {
        String ref = createAndSubmitPrintJob(appId, format, requestData, createReportRequest, createReportResponse);
        if (ref == null) {
            error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PrintWriter writer = null;
        try {
            createReportResponse.setContentType("application/json; charset=utf-8");

            writer = createReportResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_PRINT_JOB_REF).value(ref);
                String statusURL = getBaseUrl(createReportRequest) + STATUS_URL + "/" + ref + ".json";
                json.key(JSON_STATUS_LINK).value(statusURL);
                addDownloadLinkToJson(createReportRequest, ref, json);
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
     * To get the PDF created previously.
     *
     * @param referenceId       the path to the file.
     * @param inline            whether or not to inline the
     * @param getReportResponse the response object
     */
    @RequestMapping(value = REPORT_URL + "/{referenceId:\\S+}", method = RequestMethod.GET)
    public final void getReport(@PathVariable final String referenceId,
                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                final HttpServletResponse getReportResponse)
            throws IOException, ServletException {
        loadReport(referenceId, getReportResponse, new HandleReportLoadResult<Void>() {

            @Override
            public Void unsupportedLoader(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void successfulPrint(final SuccessfulPrintJob successfulPrintResult, final HttpServletResponse httpServletResponse,
                                        final URI reportURI, final ReportLoader loader) throws IOException, ServletException {
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return null;
            }

            @Override
            public Void failedPrint(final FailedPrintJob failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return null;
            }

            @Override
            public Void printJobPending(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Report has not yet completed processing", HttpStatus.ACCEPTED);
                return null;
            }
        });
    }

    /**
     * Add the print job to the job queue.
     *
     * @param format               the format of the returned report
     * @param requestData          a json formatted string with the request data required to perform the report generation.
     * @param createReportRequest  the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(@PathVariable final String format,
                                   @RequestBody final String requestData,
                                   final HttpServletRequest createReportRequest,
                                   final HttpServletResponse createReportResponse) throws JSONException {
        PJsonObject spec = parseJson(requestData, createReportResponse);

        String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
        createReport(appId, format, requestData, createReportRequest, createReportResponse);
    }

    /**
     * add the print job to the job queue.
     *
     * @param appId                the id of the app to get the request for.
     * @param format               the format of the returned report
     * @param requestData          a json formatted string with the request data required to perform the report generation.
     * @param inline               whether or not to inline the content
     * @param createReportRequest  the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGet(@PathVariable final String appId,
                                         @PathVariable final String format,
                                         @RequestBody final String requestData,
                                         @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                         final HttpServletRequest createReportRequest,
                                         final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, JSONException {

        String ref = createAndSubmitPrintJob(appId, format, requestData, createReportRequest, createReportResponse);
        if (ref == null) {
            error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
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
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
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
            isDone[0] = loadReport(ref, createReportResponse, handler);
        }
    }

    /**
     * add the print job to the job queue.
     *
     * @param format               the format of the returned report
     * @param requestData          a json formatted string with the request data required to perform the report generation.
     * @param inline               whether or not to inline the content
     * @param createReportRequest  the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGetNoAppId(@PathVariable final String format,
                                                @RequestBody final String requestData,
                                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                                final HttpServletRequest createReportRequest,
                                                final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, JSONException {
        PJsonObject spec = parseJson(requestData, createReportResponse);

        String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
        createReportAndGet(appId, format, requestData, inline, createReportRequest, createReportResponse);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param listAppsResponse the response object
     */
    @RequestMapping(value = LIST_APPS_URL, method = RequestMethod.GET)
    public final void listAppIds(final HttpServletResponse listAppsResponse) throws ServletException,
            IOException {
        Set<String> appIds = this.printerFactory.getAppIds();
        listAppsResponse.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = listAppsResponse.getWriter();

        try {
            JSONWriter json = new JSONWriter(writer);
            try {
                json.array();
                for (String appId : appIds) {
                    json.value(appId);
                }
                json.endArray();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        } finally {
            writer.close();
        }
    }


    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param pretty if true then pretty print the capabilities
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException, JSONException {
        getCapabilities(DEFAULT_CONFIGURATION_FILE_KEY, pretty, capabilitiesResponse);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param appId  the name of the "app" or in other words, a mapping to the configuration file for this request.
     * @param pretty if true then pretty print the capabilities
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = "/{appId:\\w+}" + CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @PathVariable final String appId,
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException, JSONException {
        MapPrinter printer;
        try {
            printer = this.printerFactory.create(appId);
        } catch (NoSuchAppException e) {
            error(capabilitiesResponse, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        }
        capabilitiesResponse.setContentType("application/json; charset=utf-8");
        final Writer writer;
        final ByteArrayOutputStream prettyPrintBuffer = new ByteArrayOutputStream();
        if (pretty) {
            writer = new OutputStreamWriter(prettyPrintBuffer, Constants.DEFAULT_CHARSET);
        } else {
            writer = capabilitiesResponse.getWriter();
        }

        try {
            JSONWriter json = new JSONWriter(writer);
            try {
                json.object();
                {
                    json.key(JSON_APP).value(appId);
                    printer.printClientConfig(json);
                }
                {
                    json.key("formats");
                    Set<String> formats = printer.getOutputFormatsNames();
                    json.array();
                    for (String format : formats) {
                        json.value(format);
                    }
                    json.endArray();
                }
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        } finally {
            writer.close();
        }

        if (pretty) {
            final JSONObject jsonObject = new JSONObject(new String(prettyPrintBuffer.toByteArray(), Constants.DEFAULT_CHARSET));
            capabilitiesResponse.getOutputStream().print(jsonObject.toString(JSON_INDENT_FACTOR));
    }
    }


    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(final HttpServletResponse getExampleResponse) throws ServletException, IOException {
        getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, getExampleResponse);
    }


    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param appId              the id of the app to get the request for.
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = "{appId}" + EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(
            @PathVariable final String appId,
            final HttpServletResponse getExampleResponse) throws ServletException,
            IOException {

        try {
            final MapPrinter mapPrinter = this.printerFactory.create(appId);
            final File requestDataFile = new File(mapPrinter.getConfiguration().getDirectory(), "requestData.json");
            if (requestDataFile.exists()) {
                String requestData = Files.toString(requestDataFile, Constants.DEFAULT_CHARSET);
                try {
                    final JSONObject jsonObject = new JSONObject(requestData);
                    jsonObject.remove(JSON_OUTPUT_FORMAT);
                    jsonObject.remove(JSON_APP);
                    requestData = jsonObject.toString(JSON_INDENT_FACTOR);

                    getExampleResponse.setContentType("application/json; charset=utf-8");
                } catch (JSONException e) {
                    // ignore, return raw text;

                    getExampleResponse.setContentType("text/plain; charset=utf-8");
                }
                getExampleResponse.getOutputStream().write(requestData.getBytes(Constants.DEFAULT_CHARSET));
            } else {
                getExampleResponse.getOutputStream().write(new byte[0]);
            }
        } catch (NoSuchAppException e) {
            error(getExampleResponse, "No print app identified by: " + appId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Maximum time to wait for a createAndGet request to complete before returning an error.
     *
     * @param maxCreateAndGetWaitTimeInSeconds the maximum time in seconds to wait for a report to be generated.
     */
    public final void setMaxCreateAndGetWaitTimeInSeconds(final long maxCreateAndGetWaitTimeInSeconds) {
        this.maxCreateAndGetWaitTimeInSeconds = maxCreateAndGetWaitTimeInSeconds;
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
    protected final void sendReportFile(final SuccessfulPrintJob metadata, final HttpServletResponse httpServletResponse,
                                        final ReportLoader reportLoader, final URI reportURI, final boolean inline)
            throws IOException, ServletException {

        final OutputStream response = httpServletResponse.getOutputStream();
        try {
            httpServletResponse.setContentType(metadata.getMimeType());
            if (!inline) {
                String fileName = metadata.getFileName();
                Matcher matcher = VARIABLE_PATTERN.matcher(fileName);
                while (matcher.find()) {
                    final String variable = matcher.group(1);
                    String replacement = findReplacement(variable, metadata.getCompletionDate());
                    fileName = fileName.replace("${" + variable + "}", replacement);
                    matcher = VARIABLE_PATTERN.matcher(fileName);
                }

                fileName += "." + metadata.getFileExtension();
                httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + cleanUpName(fileName));
            }
            reportLoader.loadReport(reportURI, response);
        } finally {
            response.close();
        }
    }

    private void addDownloadLinkToJson(final HttpServletRequest httpServletRequest, final String ref,
                                       final JSONWriter json) throws JSONException {
        String downloadURL = getBaseUrl(httpServletRequest) + REPORT_URL + "/" + ref;
        json.key(JSON_DOWNLOAD_LINK).value(downloadURL);
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

    private StringBuilder getBaseUrl(final HttpServletRequest httpServletRequest) {
        StringBuilder baseURL = new StringBuilder();
        if (httpServletRequest.getContextPath() != null && !httpServletRequest.getContextPath().isEmpty()) {
            baseURL.append(httpServletRequest.getContextPath());
        }
        if (httpServletRequest.getServletPath() != null && !httpServletRequest.getServletPath().isEmpty()) {
            baseURL.append(httpServletRequest.getServletPath());
        }
        return baseURL;
    }

    private PJsonObject parseJson(final String requestDataRaw, final HttpServletResponse httpServletResponse) {

        try {
            if (requestDataRaw == null) {
                error(httpServletResponse, "Missing post data.  The post payload must either be a form post with a spec parameter or " +
                                           "must " +

                                           "be a raw json post with the request.", HttpStatus.INTERNAL_SERVER_ERROR);
                return null;
            }

            String requestData = requestDataRaw;
            if (requestData.startsWith("spec=")) {
                requestData = requestData.substring("spec=".length());
            }

            try {
                return MapPrinter.parseSpec(requestData);
            } catch (RuntimeException e) {
                try {
                    return MapPrinter.parseSpec(URLDecoder.decode(requestData, Constants.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException uee) {
                    throw new RuntimeException(e);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error parsing request data: " + requestDataRaw);
            throw e;
        }
    }


    private String createAndSubmitPrintJob(final String appId, final String format, final String requestDataRaw,
                                           final HttpServletRequest httpServletRequest,
                                           final HttpServletResponse httpServletResponse) throws JSONException {

        PJsonObject specJson = parseJson(requestDataRaw, httpServletResponse);
        if (SPEC_LOGGER.isInfoEnabled()) {
            SPEC_LOGGER.info(specJson.toString());
        }
        specJson.getInternalObj().remove(JSON_OUTPUT_FORMAT);
        specJson.getInternalObj().put(JSON_OUTPUT_FORMAT, format);
        specJson.getInternalObj().remove(JSON_APP);
        specJson.getInternalObj().put(JSON_APP, appId);

        String ref = UUID.randomUUID().toString() + "@" + this.servletInfo.getServletId();

        PrintJob job = this.context.getBean(PrintJob.class);

        job.setReferenceId(ref);
        job.setRequestData(specJson);
        job.setHeaders(getHeaders(httpServletRequest));

        this.jobManager.submit(job);
        return ref;
    }

    private <R> R loadReport(final String referenceId, final HttpServletResponse httpServletResponse,
                             final HandleReportLoadResult<R> handler) throws IOException, ServletException {
        Optional<? extends CompletedPrintJob> metadata = this.jobManager.getCompletedPrintJob(referenceId);

        if (!metadata.isPresent()) {
            return handler.printJobPending(httpServletResponse, referenceId);
        } else if (metadata.get() instanceof SuccessfulPrintJob) {
            SuccessfulPrintJob successfulPrintJob = (SuccessfulPrintJob) metadata.get();
            URI pdfURI = successfulPrintJob.getURI();

            ReportLoader loader = null;
            for (ReportLoader reportLoader : this.reportLoaders) {
                if (reportLoader.accepts(pdfURI)) {
                    loader = reportLoader;
                    break;
                }
            }
            if (loader == null) {
                return handler.unsupportedLoader(httpServletResponse, referenceId);
            } else {
                return handler.successfulPrint(successfulPrintJob, httpServletResponse, pdfURI, loader);
            }
        } else if (metadata.get() instanceof FailedPrintJob) {
            FailedPrintJob failedPrintJob = (FailedPrintJob) metadata.get();
            return handler.failedPrint(failedPrintJob, httpServletResponse);
        } else {
            throw new ServletException("Unexpected state");
        }

    }

}
