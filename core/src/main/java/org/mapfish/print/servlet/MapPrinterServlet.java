package org.mapfish.print.servlet;

import net.sf.jasperreports.engine.fonts.FontFamily;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.http.matcher.UriMatchers;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.impl.PrintJobEntryImpl;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
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
     * The url path to get the list of fonts available to geotools.
     */
    public static final String FONTS_URL = "/fonts";
    /**
     * The key containing an error message for failed jobs.
     */
    public static final String JSON_ERROR = "error";
    /**
     * The application ID which indicates the configuration file to load.
     */
    public static final String JSON_APP = "app";

    /* Registry keys */
    /**
     * If the job is done (value is true) or not (value is false).
     *
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)} response.
     */
    public static final String JSON_DONE = "done";
    /**
     * The status of the job. One of the following values:
     * <ul>
     * <li>waiting</li>
     * <li>running</li>
     * <li>finished</li>
     * <li>cancelled</li>
     * <li>error</li>
     * </ul>
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)} response
     */
    public static final String JSON_STATUS = "status";
    /**
     * The elapsed time in ms from the point the job started. If the job is finished, this is the duration it
     * took to process the job.
     *
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)} response.
     */
    public static final String JSON_ELAPSED_TIME = "elapsedTime";
    /**
     * A rough estimate for the time in ms the job still has to wait in the queue until it starts processing.
     *
     * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)} response.
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
    /**
     * The JSON key in the request spec that contains the outputFormat.  This value will be put into the spec
     * by the servlet.  there is not need for the post to do this.
     */
    public static final String JSON_OUTPUT_FORMAT = "outputFormat";
    /**
     * The json tag referring to the attributes.
     */
    public static final String JSON_ATTRIBUTES = "attributes";
    /**
     * The json property to add the request headers from the print request.
     * <p>
     * The request headers from the print request are needed by certain processors, the headers are added to
     * the request JSON data for those processors.
     */
    public static final String JSON_REQUEST_HEADERS = "requestHeaders";
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPrinterServlet.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\S+)}");
    private static final int JSON_INDENT_FACTOR = 4;
    private static final List<String> REQUEST_ID_HEADERS = Arrays.asList(
            "X-Request-ID",
            "X-Correlation-ID",
            "Request-ID",
            "X-Varnish",
            "X-Amzn-Trace-Id"
    );

    static {
        Handler.configureProtocolHandler();
    }

    private final JobManager jobManager;
    private final List<ReportLoader> reportLoaders;
    private final MapPrinterFactory printerFactory;
    private final ApplicationContext context;
    private final ServletInfo servletInfo;
    private final MapPrinterFactory mapPrinterFactory;

    private long maxCreateAndGetWaitTimeInSeconds = ThreadPoolJobManager.DEFAULT_TIMEOUT_IN_SECONDS;

    @Autowired
    public MapPrinterServlet(
            final JobManager jobManager, final List<ReportLoader> reportLoaders,
            final MapPrinterFactory printerFactory, final ApplicationContext context,
            final ServletInfo servletInfo, final MapPrinterFactory mapPrinterFactory) {
        this.jobManager = jobManager;
        this.reportLoaders = reportLoaders;
        this.printerFactory = printerFactory;
        this.context = context;
        this.servletInfo = servletInfo;
        this.mapPrinterFactory = mapPrinterFactory;
    }

    /**
     * Parse the print request json data.
     *
     * @param requestDataRaw the request json in string form
     * @param httpServletResponse the response object to use for returning errors if needed
     */
    private static PJsonObject parseJson(
            final String requestDataRaw, final HttpServletResponse httpServletResponse) {

        try {
            if (requestDataRaw == null) {
                error(httpServletResponse,
                      "Missing post data.  The post payload must either be a form post with a spec " +
                              "parameter or " +
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
            LOGGER.warn("Error parsing request data: {}", requestDataRaw);
            throw e;
        }
    }

    /**
     * If the request contains a header that specifies a request ID, add it to the ref. The ref shows up in
     * every logs, that way, we can trace the request ID across applications.
     */
    private static String maybeAddRequestId(final String ref, final HttpServletRequest request) {
        final Optional<String> headerName =
                REQUEST_ID_HEADERS.stream().filter(h -> request.getHeader(h) != null).findFirst();
        return headerName.map(s -> ref + "@" + request.getHeader(s).replaceAll("[^a-zA-Z0-9._:-]", "_")
        ).orElse(ref);
    }

    /**
     * Get a status report on a job.  Returns the following json:
     *
     * <pre><code>
     *  {"time":0,"count":0,"done":false}
     * </code></pre>
     *
     * @param appId the app ID
     * @param referenceId the job reference
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param statusRequest the request object
     * @param statusResponse the response object
     */
    @RequestMapping(value = "/{appId}" + STATUS_URL + "/{referenceId:\\S+}.json", method = RequestMethod.GET)
    public final void getStatusSpecificAppId(
            @SuppressWarnings("unused") @PathVariable final String appId,
            @PathVariable final String referenceId,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest statusRequest,
            final HttpServletResponse statusResponse) {
        getStatus(referenceId, jsonpCallback, statusRequest, statusResponse);
    }

    /**
     * Get a status report on a job.  Returns the following json:
     *
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
        MDC.put(Processor.MDC_JOB_ID_KEY, referenceId);
        setNoCache(statusResponse);
        try {
            PrintJobStatus status = this.jobManager.getStatus(referenceId);

            setContentType(statusResponse, jsonpCallback);
            try (PrintWriter writer = statusResponse.getWriter()) {

                appendJsonpCallback(jsonpCallback, writer);
                JSONWriter json = new JSONWriter(writer);
                json.object();
                {
                    json.key(JSON_DONE).value(status.isDone());
                    json.key(JSON_STATUS).value(status.getStatus().toString().toLowerCase());
                    json.key(JSON_ELAPSED_TIME).value(status.getElapsedTime());
                    json.key(JSON_WAITING_TIME).value(status.getWaitingTime());
                    if (!StringUtils.isEmpty(status.getError())) {
                        json.key(JSON_ERROR).value(status.getError());
                    }

                    addDownloadLinkToJson(statusRequest, referenceId, json);
                }
                json.endObject();
                appendJsonpCallbackEnd(jsonpCallback, writer);
            }
        } catch (JSONException | IOException e) {
            throw ExceptionUtils.getRuntimeException(e);
        } catch (NoSuchReferenceException e) {
            error(statusResponse, e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Cancel a job.
     * <p>
     * Even if a job was already finished, subsequent status requests will return that the job was canceled.
     *
     * @param appId the app ID
     * @param referenceId the job reference
     * @param statusResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CANCEL_URL + "/{referenceId:\\S+}", method = RequestMethod.DELETE)
    public final void cancelSpecificAppId(
            @SuppressWarnings("unused") @PathVariable final String appId,
            @PathVariable final String referenceId,
            final HttpServletResponse statusResponse) {
        cancel(referenceId, statusResponse);
    }

    /**
     * Cancel a job.
     * <p>
     * Even if a job was already finished, subsequent status requests will return that the job was canceled.
     *
     * @param referenceId the job reference
     * @param statusResponse the response object
     */
    @RequestMapping(value = CANCEL_URL + "/{referenceId:\\S+}", method = RequestMethod.DELETE)
    public final void cancel(
            @PathVariable final String referenceId,
            final HttpServletResponse statusResponse) {
        MDC.put(Processor.MDC_JOB_ID_KEY, referenceId);
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
     * @param requestData a json formatted string with the request data required to perform the report
     *         generation.
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(
            @PathVariable final String appId,
            @PathVariable final String format,
            @RequestBody final String requestData,
            final HttpServletRequest createReportRequest,
            final HttpServletResponse createReportResponse) throws NoSuchAppException {
        setNoCache(createReportResponse);
        String ref = createAndSubmitPrintJob(appId, format, requestData, createReportRequest,
                                             createReportResponse);
        if (ref == null) {
            error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        createReportResponse.setContentType("application/json; charset=utf-8");
        try (PrintWriter writer = createReportResponse.getWriter()) {
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_PRINT_JOB_REF).value(ref);
                String statusURL = getBaseUrl(createReportRequest) + STATUS_URL + "/" + ref + ".json";
                json.key(JSON_STATUS_LINK).value(statusURL);
                addDownloadLinkToJson(createReportRequest, ref, json);
            }
            json.endObject();
        } catch (JSONException | IOException e) {
            LOGGER.warn("Error generating the JSON response", e);
        }
    }

    /**
     * To get the PDF created previously.
     *
     * @param appId the app ID
     * @param referenceId the path to the file.
     * @param inline whether or not to inline the
     * @param getReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + REPORT_URL + "/{referenceId:\\S+}", method = RequestMethod.GET)
    public final void getReportSpecificAppId(
            @SuppressWarnings("unused") @PathVariable final String appId,
            @PathVariable final String referenceId,
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
    public final void getReport(
            @PathVariable final String referenceId,
            @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
            final HttpServletResponse getReportResponse)
            throws IOException, ServletException {
        MDC.put(Processor.MDC_JOB_ID_KEY, referenceId);
        setNoCache(getReportResponse);
        loadReport(referenceId, getReportResponse, new HandleReportLoadResult<Void>() {

            @Override
            public Void unknownReference(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Error getting print with ref=" + referenceId +
                              ": unknown reference",
                      HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void unsupportedLoader(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Error getting print with ref=" + referenceId +
                              " can not be loaded",
                      HttpStatus.NOT_FOUND);
                return null;
            }

            @Override
            public Void successfulPrint(
                    final PrintJobStatus successfulPrintResult, final HttpServletResponse httpServletResponse,
                    final URI reportURI, final ReportLoader loader) throws IOException {
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return null;
            }

            @Override
            public Void failedPrint(
                    final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return null;
            }

            @Override
            public Void printJobPending(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Report has not yet completed processing", HttpStatus.ACCEPTED);
                return null;
            }
        });
    }

    /**
     * Add the print job to the job queue.
     *
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report
     *         generation.
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReport(
            @PathVariable final String format,
            @RequestBody final String requestData,
            final HttpServletRequest createReportRequest,
            final HttpServletResponse createReportResponse) throws NoSuchAppException {
        setNoCache(createReportResponse);
        PJsonObject spec = parseJson(requestData, createReportResponse);
        if (spec == null) {
            return;
        }
        final String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
        createReport(appId, format, requestData, createReportRequest, createReportResponse);
    }

    /**
     * add the print job to the job queue.
     *
     * @param appId the id of the app to get the request for.
     * @param format the format of the returned report
     * @param requestData a json formatted string with the request data required to perform the report
     *         generation.
     * @param inline whether or not to inline the content
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGet(
            @PathVariable final String appId,
            @PathVariable final String format,
            @RequestBody final String requestData,
            @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
            final HttpServletRequest createReportRequest,
            final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, NoSuchAppException {
        setNoCache(createReportResponse);

        String ref = createAndSubmitPrintJob(appId, format, requestData, createReportRequest,
                                             createReportResponse);
        if (ref == null) {
            error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        final HandleReportLoadResult<Boolean> handler = new HandleReportLoadResult<Boolean>() {

            @Override
            public Boolean unknownReference(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " unknown",
                      HttpStatus.NOT_FOUND);
                return true;
            }

            @Override
            public Boolean unsupportedLoader(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
                error(httpServletResponse, "Print with ref=" + referenceId + " can not be loaded",
                      HttpStatus.NOT_FOUND);
                return true;
            }

            @Override
            public Boolean successfulPrint(
                    final PrintJobStatus successfulPrintResult,
                    final HttpServletResponse httpServletResponse,
                    final URI reportURI, final ReportLoader loader) throws IOException {
                sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, inline);
                return true;
            }

            @Override
            public Boolean failedPrint(
                    final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
                error(httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
                return true;
            }

            @Override
            public Boolean printJobPending(
                    final HttpServletResponse httpServletResponse, final String referenceId) {
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
     * @param requestData a json formatted string with the request data required to perform the report
     *         generation.
     * @param inline whether or not to inline the content
     * @param createReportRequest the request object
     * @param createReportResponse the response object
     */
    @RequestMapping(value = CREATE_AND_GET_URL + ".{format:\\w+}", method = RequestMethod.POST)
    public final void createReportAndGetNoAppId(
            @PathVariable final String format,
            @RequestBody final String requestData,
            @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
            final HttpServletRequest createReportRequest,
            final HttpServletResponse createReportResponse)
            throws IOException, ServletException, InterruptedException, NoSuchAppException {
        setNoCache(createReportResponse);
        PJsonObject spec = parseJson(requestData, createReportResponse);
        if (spec == null) {
            return;
        }
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
        MDC.remove(Processor.MDC_JOB_ID_KEY);
        setCache(listAppsResponse);
        Set<String> appIds = this.printerFactory.getAppIds();

        setContentType(listAppsResponse, jsonpCallback);
        try (PrintWriter writer = listAppsResponse.getWriter()) {
            appendJsonpCallback(jsonpCallback, writer);

            JSONWriter json = new JSONWriter(writer);
            try {
                json.array();
                for (String appId: appIds) {
                    json.value(appId);
                }
                json.endArray();
            } catch (JSONException e) {
                throw new ServletException(e);
            }

            appendJsonpCallbackEnd(jsonpCallback, writer);
        }
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param pretty if true then pretty print the capabilities
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param request the request
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest request,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException {
        getCapabilities(DEFAULT_CONFIGURATION_FILE_KEY, pretty, jsonpCallback, request, capabilitiesResponse);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param appId the name of the "app" or in other words, a mapping to the configuration file for
     *         this request.
     * @param pretty if true then pretty print the capabilities
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param request the request
     * @param capabilitiesResponse the response object
     */
    @RequestMapping(value = "/{appId}" + CAPABILITIES_URL, method = RequestMethod.GET)
    public final void getCapabilities(
            @PathVariable final String appId,
            @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest request,
            final HttpServletResponse capabilitiesResponse) throws ServletException,
            IOException {
        MDC.remove(Processor.MDC_JOB_ID_KEY);
        setCache(capabilitiesResponse);
        MapPrinter printer;
        try {
            printer = this.printerFactory.create(appId);
            if (!checkReferer(request, printer)) {
                error(capabilitiesResponse, "Invalid referer", HttpStatus.FORBIDDEN);
                return;
            }
        } catch (NoSuchAppException e) {
            error(capabilitiesResponse, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        }

        setContentType(capabilitiesResponse, jsonpCallback);

        final ByteArrayOutputStream prettyPrintBuffer = new ByteArrayOutputStream();

        try (Writer writer = pretty ? new OutputStreamWriter(prettyPrintBuffer, Constants.DEFAULT_CHARSET) :
                capabilitiesResponse.getWriter()) {
            if (!pretty && !StringUtils.isEmpty(jsonpCallback)) {
                writer.append(jsonpCallback).append("(");
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
                    for (String format: formats) {
                        json.value(format);
                    }
                    json.endArray();
                }
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }

            if (!pretty && !StringUtils.isEmpty(jsonpCallback)) {
                writer.append(");");
            }
        }

        if (pretty) {
            final JSONObject jsonObject =
                    new JSONObject(new String(prettyPrintBuffer.toByteArray(), Constants.DEFAULT_CHARSET));

            if (!StringUtils.isEmpty(jsonpCallback)) {
                capabilitiesResponse.getOutputStream().print(jsonpCallback + "(");
            }
            capabilitiesResponse.getOutputStream().print(jsonObject.toString(JSON_INDENT_FACTOR));
            if (!StringUtils.isEmpty(jsonpCallback)) {
                capabilitiesResponse.getOutputStream().print(");");
            }
        }
    }

    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param request the request object
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest request,
            final HttpServletResponse getExampleResponse) throws IOException {
        getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, jsonpCallback, request, getExampleResponse);
    }

    /**
     * Get a sample request for the app.  An empty response may be returned if there is not example request.
     *
     * @param appId the id of the app to get the request for.
     * @param jsonpCallback if given the result is returned with a function call wrapped around it
     * @param request the request object
     * @param getExampleResponse the response object
     */
    @RequestMapping(value = "{appId}" + EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
    public final void getExampleRequest(
            @PathVariable final String appId,
            @RequestParam(value = "jsonp", defaultValue = "") final String jsonpCallback,
            final HttpServletRequest request,
            final HttpServletResponse getExampleResponse) throws
            IOException {
        MDC.remove(Processor.MDC_JOB_ID_KEY);
        setCache(getExampleResponse);
        try {
            final MapPrinter mapPrinter = this.printerFactory.create(appId);
            if (!checkReferer(request, mapPrinter)) {
                error(getExampleResponse, "Invalid referer", HttpStatus.FORBIDDEN);
                return;
            }
            final String requestDataPrefix = "requestData";
            final File[] children =
                    mapPrinter.getConfiguration().getDirectory().listFiles(
                            (dir, name) -> name.startsWith(requestDataPrefix) && name.endsWith(".json"));
            if (children == null) {
                error(getExampleResponse, "Cannot find the config directory", HttpStatus.NOT_FOUND);
                return;
            }
            JSONObject allExamples = new JSONObject();

            for (File child: children) {
                if (child.isFile()) {
                    String requestData = new String(Files.readAllBytes(child.toPath()),
                                                    Constants.DEFAULT_CHARSET);
                    try {
                        final JSONObject jsonObject = new JSONObject(requestData);
                        jsonObject.remove(JSON_OUTPUT_FORMAT);
                        jsonObject.remove(JSON_APP);
                        requestData = jsonObject.toString(JSON_INDENT_FACTOR);

                        setContentType(getExampleResponse, jsonpCallback);
                    } catch (JSONException e) {
                        // ignore, return raw text
                    }

                    String name = child.getName();
                    name = name.substring(requestDataPrefix.length());
                    if (name.startsWith("-")) {
                        name = name.substring(1);
                    }
                    name = FilenameUtils.removeExtension(name);
                    name = name.trim();
                    if (name.isEmpty()) {
                        name = FilenameUtils.removeExtension(child.getName());
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
                error(getExampleResponse, "Error translating object to json: " + e.getMessage(),
                      HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }

            try (PrintWriter writer = getExampleResponse.getWriter()) {
                appendJsonpCallback(jsonpCallback, writer);
                writer.append(result);
                appendJsonpCallbackEnd(jsonpCallback, writer);
            }
        } catch (NoSuchAppException e) {
            error(getExampleResponse, "No print app identified by: " + appId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * List the available fonts on the system.
     *
     * @return the list of available fonts in the system.  The result is a JSON Array that just lists the font
     *         family names available.
     */
    @RequestMapping(value = FONTS_URL)
    @ResponseBody
    public final String listAvailableFonts() {
        MDC.remove(Processor.MDC_JOB_ID_KEY);
        final JSONArray availableFonts = new JSONArray();
        final List<FontFamily> families =
                ExtensionsEnvironment.getExtensionsRegistry().getExtensions(FontFamily.class);
        for (FontFamily family: families) {
            availableFonts.put(family.getName());
        }
        return availableFonts.toString();
    }

    /**
     * Maximum time to wait for a createAndGet request to complete before returning an error.
     *
     * @param maxCreateAndGetWaitTimeInSeconds the maximum time in seconds to wait for a report to be
     *         generated.
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
    private void sendReportFile(
            final PrintJobStatus metadata, final HttpServletResponse httpServletResponse,
            final ReportLoader reportLoader, final URI reportURI, final boolean inline)
            throws IOException {

        try (OutputStream response = httpServletResponse.getOutputStream()) {
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
                httpServletResponse
                        .setHeader("Content-disposition", "attachment; filename=" + cleanUpName(fileName));
            }
            reportLoader.loadReport(reportURI, response);
        }
    }

    private void addDownloadLinkToJson(
            final HttpServletRequest httpServletRequest, final String ref,
            final JSONWriter json) {
        String downloadURL = getBaseUrl(httpServletRequest) + REPORT_URL + "/" + ref;
        json.key(JSON_DOWNLOAD_LINK).value(downloadURL);
    }

    /**
     * Read the headers from the request.
     *
     * @param httpServletRequest the request object
     */
    protected final JSONObject getHeaders(final HttpServletRequest httpServletRequest) {
        @SuppressWarnings("rawtypes")
        Enumeration headersName = httpServletRequest.getHeaderNames();
        JSONObject headers = new JSONObject();
        while (headersName.hasMoreElements()) {
            String name = headersName.nextElement().toString();
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
     * Start a print job.
     *
     * @param appId the id of the printer app
     * @param format the format of the returned report.
     * @param requestDataRaw the request json in string form
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     * @return the job reference id
     */
    private String createAndSubmitPrintJob(
            final String appId, final String format, final String requestDataRaw,
            final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws NoSuchAppException {

        PJsonObject specJson = parseJson(requestDataRaw, httpServletResponse);
        if (specJson == null) {
            return null;
        }
        String ref = maybeAddRequestId(
                UUID.randomUUID().toString() + "@" + this.servletInfo.getServletId(),
                httpServletRequest);
        MDC.put(Processor.MDC_JOB_ID_KEY, ref);
        LOGGER.debug("{}", specJson);

        specJson.getInternalObj().remove(JSON_OUTPUT_FORMAT);
        specJson.getInternalObj().put(JSON_OUTPUT_FORMAT, format);
        specJson.getInternalObj().remove(JSON_APP);
        specJson.getInternalObj().put(JSON_APP, appId);
        final JSONObject requestHeaders = getHeaders(httpServletRequest);
        if (requestHeaders.length() > 0) {
            specJson.getInternalObj().getJSONObject(JSON_ATTRIBUTES)
                    .put(JSON_REQUEST_HEADERS, requestHeaders);
        }

        // check that we have authorization and configure the job so it can only be access by users with
        // sufficient authorization
        final String templateName = specJson.getString(Constants.JSON_LAYOUT_KEY);
        final MapPrinter mapPrinter = this.mapPrinterFactory.create(appId);
        checkReferer(httpServletRequest, mapPrinter);
        final Template template = mapPrinter.getConfiguration().getTemplate(templateName);
        if (template == null) {
            return null;
        }

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

    private boolean checkReferer(
            final HttpServletRequest request, final MapPrinter mapPrinter) {
        final Configuration config = mapPrinter.getConfiguration();
        final UriMatchers allowedReferers = config.getAllowedReferersImpl();
        if (allowedReferers == null) {
            return true;
        }
        String referer = request.getHeader("referer");
        if (referer == null) {
            referer = "http://localhost/";
        }
        try {
            return allowedReferers.matches(new URI(referer),
                                           HttpMethod.resolve(request.getMethod()));
        } catch (SocketException | UnknownHostException | URISyntaxException | MalformedURLException e) {
            LOGGER.error("Referer {} invalid", referer, e);
            return false;
        }
    }

    private <R> R loadReport(
            final String referenceId, final HttpServletResponse httpServletResponse,
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
            for (ReportLoader reportLoader: this.reportLoaders) {
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

    private void setContentType(
            final HttpServletResponse statusResponse,
            final String jsonpCallback) {
        if (StringUtils.isEmpty(jsonpCallback)) {
            statusResponse.setContentType("application/json; charset=utf-8");
        } else {
            statusResponse.setContentType("application/javascript; charset=utf-8");
        }
    }

    private void appendJsonpCallback(
            final String jsonpCallback,
            final PrintWriter writer) {
        if (!StringUtils.isEmpty(jsonpCallback)) {
            writer.append(jsonpCallback);
            writer.append("(");
        }
    }

    private void appendJsonpCallbackEnd(
            final String jsonpCallback,
            final PrintWriter writer) {
        if (!StringUtils.isEmpty(jsonpCallback)) {
            writer.append(");");
        }
    }

}
