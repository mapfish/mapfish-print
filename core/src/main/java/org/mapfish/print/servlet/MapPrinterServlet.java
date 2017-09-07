package org.mapfish.print.servlet;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.impl.PrintJobEntryImpl;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.GraphicsEnvironment;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
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
 */
@Controller
public class MapPrinterServlet extends BaseMapServlet {

    static {
        Handler.configureProtocolHandler();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MapPrinterServlet.class);

    /**
     * The url path for capabilities requests.
     */
    public static final String CAPABILITIES_URL = "/capabilities.json";
    /**
     * The url path to list all registered configurations.
     */
    public static final String LIST_APPS_URL = "/apps.json";
    /**
     * The url path to get a sample print request.
     */
    public static final String EXAMPLE_REQUEST_URL = "/exampleRequest.json";
    /**
     * The url path to create and get a report.
     */
    public static final String CREATE_AND_GET_URL = "/buildreport";
    /**
     * The url path to get the status for a print task.
     */
    public static final String STATUS_URL = "/status";
    /**
     * The url path to cancel a print task.
     */
    public static final String CANCEL_URL = "/cancel";
    /**
     * The url path to create a print task and to get a finished print.
     */
    public static final String REPORT_URL = "/report";
    /**
     * The url path to create a print task and to get a finished print.
     */
    public static final String FONTS_URL = "/fonts";

    /* Registry keys */

    /**
     * The key containing an error message for failed jobs.
     */
    public static final String JSON_ERROR = "error";
    /**
     * The application ID which indicates the configuration file to load.
     */
    public static final String JSON_APP = "app";
    /**
     * The json property name of the property that contains the request spec.
     */
    public static final String JSON_SPEC = "spec";
    /**
     * If the job is done (value is true) or not (value is false).
     * <p></p>
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * response.
     */
    public static final String JSON_DONE = "done";
    /**
     * The status of the job. One of the following values:
     * <ul>
     *  <li>waiting</li>
     *  <li>running</li>
     *  <li>finished</li>
     *  <li>cancelled</li>
     *  <li>error</li>
     * </ul>
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * response
     */
    public static final String JSON_STATUS = "status";
    /**
     * The elapsed time in ms from the point the job started. If the job is finished,
     * this is the duration it took to process the job.
     * <p></p>
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * response.
     */
    public static final String JSON_ELAPSED_TIME = "elapsedTime";
    /**
     * A rough estimate for the time in ms the job still has to wait in the queue until it starts processing.
     * <p></p>
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * response.
     */
    public static final String JSON_WAITING_TIME = "waitingTime";
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
    /**
     * The json tag referring to the attributes.
     */
    public static final String JSON_ATTRIBUTES = "attributes";
    /**
     * The json property to add the request headers from the print request.
     *
     * The request headers from the print request are needed by certain processors,
     * the headers are added to the request JSON data for those processors.
     */
    public static final String JSON_REQUEST_HEADERS = "requestHeaders";

    private static final List<String> REQUEST_ID_HEADERS = Arrays.asList(
            "X-Request-ID",
            "X-Correlation-ID",
            "Request-ID",
            "X-Varnish",
            "X-Amzn-Trace-Id"
    );

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
    @Autowired
    private MapPrinterFactory mapPrinterFactory;


    /**
     * Get a status report on a job.  Returns the following json:
     * <p></p>
     * <pre><code>
     *  {"time":0,"count":0,"done":false}
     * </code></pre>
     *
     * @param referenceId the job reference
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param statusRequest the request object
     * @param statusResponse the response object
     */
    @RequestMapping(value = "/{appId}" + STATUS_URL + "/{referenceId:\\S+}.json", method = RequestMethod.GET)
    public final void getStatusSpecificAppId(
            @PathVariable final String referenceId,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest statusRequest,
            final HttpServletResponse statusResponse) {
        getStatus(referenceId, jsonpCallback, statusRequest, statusResponse);
    }
    /**
     * Get a status report on a job.  Returns the following json:
     * <p></p>
     * <pre><code>
     *  {"time":0,"count":0,"done":false}
     * </code></pre>
     *
     * @param referenceId the job reference
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param statusRequest the request object
     * @param statusResponse the response object
     */
    @RequestMapping(value = STATUS_URL + "/{referenceId:\\S+}.json", method = RequestMethod.GET)
    public final void getStatus(
            @PathVariable final String referenceId,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest statusRequest,
            final HttpServletResponse statusResponse) {
        setNoCache(statusResponse);
        PrintWriter writer = null;
        try {
            PrintJobStatus status = this.jobManager.getStatus(referenceId);

            setContentType(statusResponse, jsonpCallback);
            writer = statusResponse.getWriter();

            appendJsonpCallback(jsonpCallback, writer);
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_DONE).value(status.isDone());
                json.key(JSON_STATUS).value(status.getStatus().toString().toLowerCase());
                json.key(JSON_ELAPSED_TIME).value(status.getElapsedTime());
                json.key(JSON_WAITING_TIME).value(status.getWaitingTime());
                if (!Strings.isNullOrEmpty(status.getError())) {
                    json.key(JSON_ERROR).value(status.getError());
                }

                addDownloadLinkToJson(statusRequest, referenceId, json);
            }
            json.endObject();
            appendJsonpCallbackEnd(jsonpCallback, writer);
        } catch (JSONException e) {
            LOGGER.error("Error obtaining status", e);
            throw ExceptionUtils.getRuntimeException(e);
        } catch (IOException e) {
            LOGGER.error("Error obtaining status", e);
            throw ExceptionUtils.getRuntimeException(e);
        } catch (NoSuchReferenceException e) {
            error(statusResponse, e.getMessage(), HttpStatus.NOT_FOUND);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Cancel a job.
     *
     * Even if a job was already finished, subsequent status requests will
     * return that the job was canceled.
     *
     * @param referenceId the job reference
     * @param statusResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CANCEL_URL + "/{referenceId:\\S+}", method = RequestMethod.DELETE)
    public final void cancelSpecificAppId(
            @PathVariable final String referenceId,
            final HttpServletResponse statusResponse) {
        cancel(referenceId, statusResponse);
    }

    /**
     * Cancel a job.
     *
     * Even if a job was already finished, subsequent status requests will
     * return that the job was canceled.
     *
     * @param referenceId the job reference
     * @param statusResponse the response object
     */
    @RequestMapping(value = CANCEL_URL + "/{referenceId:\\S+}", method = RequestMethod.DELETE)
    public final void cancel(
            @PathVariable final String referenceId,
            final HttpServletResponse statusResponse) {
        setNoCache(statusResponse);
        try {
            this.jobManager.cancel(referenceId);
        } catch (NoSuchReferenceException e) {
            error(statusResponse, e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Add the print job to the job queue.
     *
     * @param appId the id of the app to get the request for.
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report generation.
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(@PathVariable final String appId,
                                   @PathVariable final String format,
                                   @RequestBody final String requestData,
                                   final HttpServletRequest createReportRequest,
                                   final HttpServletResponse createReportResponse) throws JSONException, NoSuchAppException {
        setNoCache(createReportResponse);
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
     * @param referenceId the path to the file.
     * @param inline whether or not to inline the
     * @param getReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + REPORT_URL + "/{referenceId:\\S+}", method = RequestMethod.GET)
    public final void getReportSpecificAppId(@PathVariable final String referenceId,
                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                final HttpServletResponse getReportResponse)
            throws IOException, ServletException {
        getReport(referenceId, inline, getReportResponse);
    }
    /**
     * To get the PDF created previously.
     *
     * @param referenceId the path to the file.
     * @param inline whether or not to inline the
     * @param getReportResponse the response object
     */
    @RequestMapping(value = REPORT_URL + "/{referenceId:\\S+}", method = RequestMethod.GET)
    public final void getReport(@PathVariable final String referenceId,
                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                final HttpServletResponse getReportResponse)
            throws IOException, ServletException {
        setNoCache(getReportResponse);
        loadReport(referenceId, getReportResponse, new HandleReportLoadResult<Void>() {

            @Override
            public Void unknownReference(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void unsupportedLoader(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " can not be loaded", HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void successfulPrint(final PrintJobStatus successfulPrintResult, final HttpServletResponse httpServletResponse,
                                        final URI reportURI, final ReportLoader loader) throws IOException, ServletException {
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return null;
            }

            @Override
            public Void failedPrint(final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
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
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report generation.
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(@PathVariable final String format,
                                   @RequestBody final String requestData,
                                   final HttpServletRequest createReportRequest,
                                   final HttpServletResponse createReportResponse) throws JSONException, NoSuchAppException {
        setNoCache(createReportResponse);
        PJsonObject spec = parseJson(requestData, createReportResponse);

        String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
        createReport(appId, format, requestData, createReportRequest, createReportResponse);
    }

    /**
     * add the print job to the job queue.
     *
     * @param appId the id of the app to get the request for.
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report generation.
     * @param inline whether or not to inline the content
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGet(@PathVariable final String appId,
                                         @PathVariable final String format,
                                         @RequestBody final String requestData,
                                         @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                         final HttpServletRequest createReportRequest,
                                         final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, JSONException, NoSuchAppException {
        setNoCache(createReportResponse);

        String ref = createAndSubmitPrintJob(appId, format, requestData, createReportRequest, createReportResponse);
        if (ref == null) {
            error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        final HandleReportLoadResult<Boolean> handler = new HandleReportLoadResult<Boolean>() {

            @Override
            public Boolean unknownReference(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
                return true;
            }

            @Override
            public Boolean unsupportedLoader(final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " can not be loaded", HttpStatus.NOT_FOUND);
                return true;
            }

            @Override
            public Boolean successfulPrint(final PrintJobStatus successfulPrintResult,
                                           final HttpServletResponse httpServletResponse,
                                           final URI reportURI, final ReportLoader loader) throws IOException, ServletException {
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return true;
            }

            @Override
            public Boolean failedPrint(final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return true;
            }

            @Override
            public Boolean printJobPending(final HttpServletResponse httpServletResponse, final String referenceId) {
                return false;
            }
        };


        boolean isDone = false;
        long startWaitTime = System.currentTimeMillis();
        final long maxWaitTimeInMillis = TimeUnit.SECONDS.toMillis(this.maxCreateAndGetWaitTimeInSeconds);
        while (!isDone && System.currentTimeMillis() - startWaitTime < maxWaitTimeInMillis) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            isDone = loadReport(ref, createReportResponse, handler);
        }
    }

    /**
     * add the print job to the job queue.
     *
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report generation.
     * @param inline whether or not to inline the content
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGetNoAppId(@PathVariable final String format,
                                                @RequestBody final String requestData,
                                                @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                                                final HttpServletRequest createReportRequest,
                                                final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, JSONException, NoSuchAppException {
        setNoCache(createReportResponse);
        PJsonObject spec = parseJson(requestData, createReportResponse);

        String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
        createReportAndGet(appId, format, requestData, inline, createReportRequest, createReportResponse);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param listAppsResponse the response object
     */
    @RequestMapping(value = LIST_APPS_URL, method = RequestMethod.GET)
    public final void listAppIds(
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletResponse listAppsResponse) throws ServletException,
            IOException {
        setCache(listAppsResponse);
        Set<String> appIds = this.printerFactory.getAppIds();

        setContentType(listAppsResponse, jsonpCallback);
        final PrintWriter writer = listAppsResponse.getWriter();
        try {
            appendJsonpCallback(jsonpCallback, writer);

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

            appendJsonpCallbackEnd(jsonpCallback, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param pretty if true then pretty print the capabilities
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException, JSONException {
        getCapabilities(DEFAULT_CONFIGURATION_FILE_KEY, pretty, jsonpCallback, capabilitiesResponse);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param appId the name of the "app" or in other words, a mapping to the configuration file for this request.
     * @param pretty if true then pretty print the capabilities
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @PathVariable final String appId,
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException, JSONException {
        setCache(capabilitiesResponse);
        MapPrinter printer;
        try {
            printer = this.printerFactory.create(appId);
        } catch (NoSuchAppException e) {
            error(capabilitiesResponse, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        }

        setContentType(capabilitiesResponse, jsonpCallback);

        final Writer writer;
        final ByteArrayOutputStream prettyPrintBuffer = new ByteArrayOutputStream();
        if (pretty) {
            writer = new OutputStreamWriter(prettyPrintBuffer, Constants.DEFAULT_CHARSET);
        } else {
            writer = capabilitiesResponse.getWriter();
        }

        try {
            if (!pretty && !Strings.isNullOrEmpty(jsonpCallback)) {
                writer.append(jsonpCallback + "(");
            }

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

            if (!pretty && !Strings.isNullOrEmpty(jsonpCallback)) {
                writer.append(");");
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        if (pretty) {
            final JSONObject jsonObject = new JSONObject(new String(prettyPrintBuffer.toByteArray(), Constants.DEFAULT_CHARSET));

            if (!Strings.isNullOrEmpty(jsonpCallback)) {
                capabilitiesResponse.getOutputStream().print(jsonpCallback + "(");
            }
            capabilitiesResponse.getOutputStream().print(jsonObject.toString(JSON_INDENT_FACTOR));
            if (!Strings.isNullOrEmpty(jsonpCallback)) {
                capabilitiesResponse.getOutputStream().print(");");
            }
        }
    }


    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletResponse getExampleResponse) throws ServletException, IOException {
        getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, jsonpCallback, getExampleResponse);
    }


    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param appId the id of the app to get the request for.
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = "{appId}" + EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(
            @PathVariable final String appId,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletResponse getExampleResponse) throws ServletException,
            IOException {
        setCache(getExampleResponse);
        PrintWriter writer = null;
        try {
            final MapPrinter mapPrinter = this.printerFactory.create(appId);
            final Iterable<File> children = Files.fileTreeTraverser().children(mapPrinter.getConfiguration().getDirectory());
            JSONObject allExamples = new JSONObject();

            for (File child : children) {
                final String requestDataPrefix = "requestData";
                if (child.isFile() && child.getName().startsWith(requestDataPrefix) && child.getName().endsWith(".json")) {
                    String requestData = Files.toString(child, Constants.DEFAULT_CHARSET);
                    try {
                        final JSONObject jsonObject = new JSONObject(requestData);
                        jsonObject.remove(JSON_OUTPUT_FORMAT);
                        jsonObject.remove(JSON_APP);
                        requestData = jsonObject.toString(JSON_INDENT_FACTOR);

                        setContentType(getExampleResponse, jsonpCallback);
                    } catch (JSONException e) {
                        // ignore, return raw text;
                    }

                    String name = child.getName();
                    name = name.substring(requestDataPrefix.length());
                    if (name.startsWith("-")) {
                        name = name.substring(1);
                    }
                    name = Files.getNameWithoutExtension(name);
                    name = name.trim();
                    if (name.isEmpty()) {
                        name = Files.getNameWithoutExtension(child.getName());
                    }

                    try {
                        allExamples.put(name, requestData);
                    } catch (JSONException e) {
                        Log.error("Error translating object to json", e);
                        error(getExampleResponse, "Error translating object to json: " + e.getMessage(),
                                HttpStatus.INTERNAL_SERVER_ERROR);
                        return;
                    }
                }
            }
            final String result;
            try {
                result = allExamples.toString(JSON_INDENT_FACTOR);
            } catch (JSONException e) {
                Log.error("Error translating object to json", e);
                error(getExampleResponse, "Error translating object to json: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            writer = getExampleResponse.getWriter();
            appendJsonpCallback(jsonpCallback, writer);
            writer.append(result);
            appendJsonpCallbackEnd(jsonpCallback, writer);
        } catch (NoSuchAppException e) {
            error(getExampleResponse, "No print app identified by: " + appId, HttpStatus.NOT_FOUND);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * List the available fonts on the system.
     *
     * @return the list of available fonts in the system.  The result is a JSON Array that just lists the font family names available.
     */
    @RequestMapping(value = FONTS_URL)
    @ResponseBody
    public final String listAvailableFonts() {
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        JSONArray availableFonts = new JSONArray();
        for (String font : e.getAvailableFontFamilyNames()) {
            availableFonts.put(font);
        }
        return availableFonts.toString();
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
     * @param metadata the client request data
     * @param httpServletResponse the response object
     * @param reportLoader the object used for loading the report
     * @param reportURI the uri of the report
     * @param inline whether or not to inline the content
     */
    protected final void sendReportFile(final PrintJobStatus metadata, final HttpServletResponse httpServletResponse,
                                        final ReportLoader reportLoader, final URI reportURI, final boolean inline)
            throws IOException, ServletException {

        final OutputStream response = httpServletResponse.getOutputStream();
        try {
            httpServletResponse.setContentType(metadata.getResult().getMimeType());
            if (!inline) {
                String fileName = metadata.getResult().getFileName();
                Matcher matcher = VARIABLE_PATTERN.matcher(fileName);
                while (matcher.find()) {
                    final String variable = matcher.group(1);
                    String replacement = findReplacement(variable, metadata.getCompletionDate());
                    fileName = fileName.replace("${" + variable + "}", replacement);
                    matcher = VARIABLE_PATTERN.matcher(fileName);
                }

                fileName += "." + metadata.getResult().getFileExtension();
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
    protected final JSONObject getHeaders(final HttpServletRequest httpServletRequest) throws JSONException {
        @SuppressWarnings("rawtypes")
        Enumeration headersName = httpServletRequest.getHeaderNames();
        JSONObject headers = new JSONObject();
        while (headersName.hasMoreElements()) {
            String name = headersName.nextElement().toString();
            @SuppressWarnings("unchecked")
            Enumeration<String> e = httpServletRequest.getHeaders(name);
            while (e.hasMoreElements()) {
                headers.append(name, e.nextElement());
            }

        }
        final JSONObject requestHeadersAttribute = new JSONObject();
        requestHeadersAttribute.put(JSON_REQUEST_HEADERS, headers);
        return requestHeadersAttribute;
    }

    /**
     * Parse the print request json data.
     *
     * @param requestDataRaw the request json in string form
     * @param httpServletResponse the response object to use for returning errors if needed
     */
    public static PJsonObject parseJson(final String requestDataRaw, final HttpServletResponse httpServletResponse) {

        try {
            if (requestDataRaw == null) {
                error(httpServletResponse, "Missing post data.  The post payload must either be a form post with a spec parameter or " +
                                           "must be a raw json post with the request.", HttpStatus.INTERNAL_SERVER_ERROR);
                return null;
            }

            String requestData = requestDataRaw;
            if (!requestData.startsWith("spec=") && !requestData.startsWith("{")) {
                try {
                    requestData = URLDecoder.decode(requestData, Constants.DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }
            if (requestData.startsWith("spec=")) {
                requestData = requestData.substring("spec=".length());
            }


            try {
                return MapPrinter.parseSpec(requestData);
            } catch (RuntimeException e) {
                try {
                    return MapPrinter.parseSpec(URLDecoder.decode(requestData, Constants.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException uee) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error parsing request data: " + requestDataRaw);
            throw e;
        }
    }

    /**
     * Start a print job.
     *
     * @param appId the id of the printer app
     * @param format the format of the returned report.
     * @param requestDataRaw the request json in string form
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     * @return the job reference id
     */
    public final String createAndSubmitPrintJob(
            final String appId, final String format, final String requestDataRaw,
            final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws JSONException, NoSuchAppException {

        PJsonObject specJson = parseJson(requestDataRaw, httpServletResponse);
        if (specJson == null) {
            return null;
        }
        String ref = maybeAddRequestId(
                UUID.randomUUID().toString() + "@" + this.servletInfo.getServletId(),
                httpServletRequest);
        MDC.put("job_id", ref);
        LOGGER.debug("\nspec:\n{}", specJson);

        specJson.getInternalObj().remove(JSON_OUTPUT_FORMAT);
        specJson.getInternalObj().put(JSON_OUTPUT_FORMAT, format);
        specJson.getInternalObj().remove(JSON_APP);
        specJson.getInternalObj().put(JSON_APP, appId);
        final JSONObject requestHeaders = getHeaders(httpServletRequest);
        if (requestHeaders.length() > 0) {
            specJson.getInternalObj().getJSONObject(JSON_ATTRIBUTES).put(JSON_REQUEST_HEADERS, requestHeaders);
        }

        // check that we have authorization and configure the job so it can only be access by users with sufficient authorization
        final String templateName = specJson.getString(Constants.JSON_LAYOUT_KEY);
        final MapPrinter mapPrinter = this.mapPrinterFactory.create(appId);
        final Template template = mapPrinter.getConfiguration().getTemplate(templateName);

        PrintJobEntryImpl jobEntry = new PrintJobEntryImpl(ref, specJson, System.currentTimeMillis());
        jobEntry.configureAccess(template, this.context);

        try {
            this.jobManager.submit(jobEntry);
        } catch (RuntimeException exc) {
            LOGGER.error("Error when creating job on {}: {}", appId, specJson, exc);
            ref = null;
        }
        return ref;
    }

    /**
     * If the request contains a header that specifies a request ID, add it to the ref. The ref shows up
     * in every logs, that way, we can trace the request ID across applications.
     */
    private static String maybeAddRequestId(final String ref, final HttpServletRequest request) {
        final Optional<String> headerName =
                REQUEST_ID_HEADERS.stream().filter(h -> request.getHeader(h) != null).findFirst();
        return headerName.map(s ->
                ref + "@" + request.getHeader(s).replaceAll("[^a-zA-Z0-9._-]", "_")
        ).orElse(
                ref
        );
    }

    private <R> R loadReport(final String referenceId, final HttpServletResponse httpServletResponse,
                             final HandleReportLoadResult<R> handler) throws IOException, ServletException {
        PrintJobStatus metadata;

        try {
            metadata = this.jobManager.getStatus(referenceId);
        } catch (NoSuchReferenceException e) {
            return handler.unknownReference(httpServletResponse, referenceId);
        }

        if (!metadata.isDone()) {
            return handler.printJobPending(httpServletResponse, referenceId);
        } else if (metadata.getResult() != null) {
            URI pdfURI = metadata.getResult().getReportURI();

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
                return handler.successfulPrint(metadata, httpServletResponse, pdfURI, loader);
            }
        } else {
            return handler.failedPrint(metadata, httpServletResponse);
        }

    }

    private void setContentType(final HttpServletResponse statusResponse,
                                final String jsonpCallback) {
        if (Strings.isNullOrEmpty(jsonpCallback)) {
            statusResponse.setContentType("application/json; charset=utf-8");
        } else {
            statusResponse.setContentType("application/javascript; charset=utf-8");
        }
    }

    private void appendJsonpCallback(final String jsonpCallback,
                                     final PrintWriter writer) {
        if (!Strings.isNullOrEmpty(jsonpCallback)) {
            writer.append(jsonpCallback + "(");
        }
    }

    private void appendJsonpCallbackEnd(final String jsonpCallback,
                                        final PrintWriter writer) {
        if (!Strings.isNullOrEmpty(jsonpCallback)) {
            writer.append(");");
        }
    }

}
