package org.mapfish.print.servlet;

import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

import io.sentry.Sentry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.fonts.FontFamily;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.FontTools;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.PrintException;
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

/** The default servlet. */
@Controller
public class MapPrinterServlet extends BaseMapServlet {

  /** The url path for capabilities requests. */
  public static final String CAPABILITIES_URL = "/capabilities.json";

  /** The url path to list all registered configurations. */
  public static final String LIST_APPS_URL = "/apps.json";

  /** The url path to get a sample print request. */
  public static final String EXAMPLE_REQUEST_URL = "/exampleRequest.json";

  /** The url path to create and get a report. */
  public static final String CREATE_AND_GET_URL = "/buildreport";

  /** The url path to get the status for a print task. */
  public static final String STATUS_URL = "/status";

  /** The url path to cancel a print task. */
  public static final String CANCEL_URL = "/cancel";

  /** The url path to create a print task and to get a finished print. */
  public static final String REPORT_URL = "/report";

  /** The url path to get the list of fonts available to geotools. */
  public static final String FONTS_URL = "/fonts";

  /** The key containing an error message for failed jobs. */
  public static final String JSON_ERROR = "error";

  /** The application ID which indicates the configuration file to load. */
  public static final String JSON_APP = "app";

  /* Registry keys */
  /**
   * If the job is done (value is true) or not (value is false).
   *
   * <p>Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)} response.
   */
  public static final String JSON_DONE = "done";

  /**
   * The status of the job. One of the following values:
   *
   * <ul>
   *   <li>waiting
   *   <li>running
   *   <li>finished
   *   <li>canceled
   *   <li>error
   * </ul>
   *
   * Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)} response
   */
  public static final String JSON_STATUS = "status";

  /**
   * The elapsed time in ms from the point the job started. If the job is finished, this is the
   * duration it took to process the job.
   *
   * <p>Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)} response.
   */
  public static final String JSON_ELAPSED_TIME = "elapsedTime";

  /**
   * A rough estimate for the time in ms the job still has to wait in the queue until it starts
   * processing.
   *
   * <p>Part of the {@link #getStatus(String, String, javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)} response.
   */
  public static final String JSON_WAITING_TIME = "waitingTime";

  /** The key containing the print job reference ID in the create report response. */
  public static final String JSON_PRINT_JOB_REF = "ref";

  /**
   * The json key in the create report response containing a link to get the status of the print
   * job.
   */
  public static final String JSON_STATUS_LINK = "statusURL";

  /**
   * The json key in the create report and status responses containing a link to download the
   * report.
   */
  public static final String JSON_DOWNLOAD_LINK = "downloadURL";

  /**
   * The JSON key in the request spec that contains the outputFormat. This value will be put into
   * the spec by the servlet. there is not need for the post to do this.
   */
  public static final String JSON_OUTPUT_FORMAT = "outputFormat";

  /** The json tag referring to the attributes. */
  public static final String JSON_ATTRIBUTES = "attributes";

  /**
   * The json property to add the request headers from the print request.
   *
   * <p>The request headers from the print request are needed by certain processors, the headers are
   * added to the request JSON data for those processors.
   */
  public static final String JSON_REQUEST_HEADERS = "requestHeaders";

  /** The JSON key in the request spec that contains the Jasper report fonts. */
  public static final String JSON_OUTPUT_JASPERREPORT_FONTS = "jasperreportFonts";

  /** The JSON key in the request spec that contains the font config fonts. */
  public static final String JSON_OUTPUT_FONTS = "fonts";

  /** The JSON key in the request spec that contains the java font family name. */
  public static final String JSON_OUTPUT_FONT_FAMILY = "family";

  /** The JSON key in the request spec that contains the font families names. */
  public static final String JSON_OUTPUT_FONTCONFIG = "fontconfig";

  /** The JSON key in the request spec that contains the font families names. */
  public static final String JSON_OUTPUT_FONTCONFIG_FAMILIES = "families";

  /** The JSON key in the request spec that contains the font name. */
  public static final String JSON_OUTPUT_FONTCONFIG_NAME = "name";

  /** The JSON key in the request spec that contains the font styles. */
  public static final String JSON_OUTPUT_FONTCONFIG_STYLES = "styles";

  /** The JSON key in the request spec that contains the font weight. */
  public static final String JSON_OUTPUT_FONTCONFIG_WEIGHT = "weight";

  private static final Logger LOGGER = LoggerFactory.getLogger(MapPrinterServlet.class);
  private static final int JSON_INDENT_FACTOR = 4;
  private static final List<String> REQUEST_ID_HEADERS =
      Arrays.asList(
          "X-Request-ID", "X-Correlation-ID", "Request-ID", "X-Varnish", "X-Amzn-Trace-Id");

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
      final JobManager jobManager,
      final List<ReportLoader> reportLoaders,
      final MapPrinterFactory printerFactory,
      final ApplicationContext context,
      final ServletInfo servletInfo,
      final MapPrinterFactory mapPrinterFactory) {
    this.jobManager = jobManager;
    this.reportLoaders = reportLoaders;
    this.printerFactory = printerFactory;
    this.context = context;
    this.servletInfo = servletInfo;
    this.mapPrinterFactory = mapPrinterFactory;

    boolean enableSentry = System.getProperties().contains("sentry.dsn");
    enableSentry |= System.getenv().containsKey("SENTRY_URL");
    enableSentry |= System.getenv().containsKey("SENTRY_DSN");
    if (enableSentry) {
      Sentry.init(
          options -> {
            options.setEnableExternalConfiguration(true);
            options.setBeforeSend(
                (event, hint) -> {
                  LOGGER.info(
                      "Sentry event, logger: {}, message: {}",
                      event.getLogger(),
                      event.getMessage() != null ? event.getMessage().getMessage() : null);
                  if (Objects.equals(
                          event.getLogger(), "org.hibernate.engine.jdbc.spi.SqlExceptionHelper")
                      && ((event.getMessage() != null)
                          && (Objects.equals(
                                  event.getMessage().getMessage(),
                                  "ERROR: could not obtain lock on row in relation"
                                      + " \"print_job_statuses\"")
                              || Objects.equals(
                                  event.getMessage().getMessage(),
                                  "SQL Error: 0, SQLState: 55P03")))) {
                    return null;
                  }
                  return event;
                });
          });
    }
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
        error(
            httpServletResponse,
            "Missing post data.  The post payload must either be a form post with a spec "
                + "parameter or "
                + "must be a raw json post with the request.",
            HttpStatus.INTERNAL_SERVER_ERROR);
        return null;
      }

      String requestData = requestDataRaw;
      if (!requestData.startsWith("spec=") && !requestData.startsWith("{")) {
        try {
          requestData = URLDecoder.decode(requestData, Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
          throw createPrintException(e, requestData);
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
          throw createPrintException(uee, requestData);
        }
      }
    } catch (RuntimeException e) {
      LOGGER.warn("Error parsing request data: {}", requestDataRaw);
      throw e;
    }
  }

  private static PrintException createPrintException(
      final UnsupportedEncodingException e, final String requestData) {
    String message =
        String.format("Failed to decode %s using %s", requestData, Constants.DEFAULT_ENCODING);
    return new PrintException(message, e);
  }

  /**
   * If the request contains a header that specifies a request ID, add it to the ref. The ref shows
   * up in every logs, that way, we can trace the request ID across applications.
   */
  private static String maybeAddRequestId(final String ref, final HttpServletRequest request) {
    final Optional<String> headerName =
        REQUEST_ID_HEADERS.stream().filter(h -> request.getHeader(h) != null).findFirst();
    return headerName
        .map(s -> ref + "@" + request.getHeader(s).replaceAll("[^a-zA-Z0-9._:-]", "_"))
        .orElse(ref);
  }

  /**
   * Get a status report on a job. Returns the following json:
   *
   * <pre><code>
   *  {"time":0,"count":0,"done":false}
   * </code></pre>
   *
   * @param appId the app ID
   * @param referenceId the job reference
   * @param statusRequest the request object
   * @param statusResponse the response object
   */
  @RequestMapping(
      value = "/{appId}" + STATUS_URL + "/{referenceId:\\S+}.json",
      method = RequestMethod.GET)
  public final void getStatusSpecificAppId(
      @Nonnull @PathVariable final String appId,
      @Nonnull @PathVariable final String referenceId,
      final HttpServletRequest statusRequest,
      final HttpServletResponse statusResponse) {
    getStatus(appId, referenceId, statusRequest, statusResponse);
  }

  /**
   * Get a status report on a job. Returns the following json:
   *
   * <pre><code>
   *  {"time":0,"count":0,"done":false}
   * </code></pre>
   *
   * @param referenceId the job reference
   * @param statusRequest the request object
   * @param statusResponse the response object
   */
  @RequestMapping(value = STATUS_URL + "/{referenceId:\\S+}.json", method = RequestMethod.GET)
  public final void getStatusPath(
      @Nonnull @PathVariable final String referenceId,
      final HttpServletRequest statusRequest,
      final HttpServletResponse statusResponse) {
    getStatus("default", referenceId, statusRequest, statusResponse);
  }

  /**
   * Get a status report on a job. Returns the following json:
   *
   * <pre><code>
   *  {"time":0,"count":0,"done":false}
   * </code></pre>
   *
   * @param applicationId the application ID
   * @param referenceId the job reference
   * @param statusRequest the request object
   * @param statusResponse the response object
   */
  public final void getStatus(
      @Nonnull final String applicationId,
      @Nonnull final String referenceId,
      final HttpServletRequest statusRequest,
      final HttpServletResponse statusResponse) {
    MDC.put(Processor.MDC_APPLICATION_ID_KEY, applicationId);
    MDC.put(Processor.MDC_JOB_ID_KEY, referenceId);
    setNoCache(statusResponse);
    try {
      PrintJobStatus status = this.jobManager.getStatus(referenceId);

      setContentType(statusResponse);
      try (PrintWriter writer = statusResponse.getWriter()) {
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
      }
    } catch (IOException e) {
      throw new PrintException("Failed to get writer from " + statusResponse, e);
    } catch (NoSuchReferenceException e) {
      error(statusResponse, e.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Cancel a job.
   *
   * <p>Even if a job was already finished, subsequent status requests will return that the job was
   * canceled.
   *
   * @param appId the app ID
   * @param referenceId the job reference
   * @param statusResponse the response object
   */
  @RequestMapping(
      value = "/{appId}" + CANCEL_URL + "/{referenceId:\\S+}",
      method = RequestMethod.DELETE)
  public final void cancelSpecificAppId(
      @Nonnull @PathVariable final String appId,
      @Nonnull @PathVariable final String referenceId,
      final HttpServletResponse statusResponse) {
    cancel(appId, referenceId, statusResponse);
  }

  /**
   * Cancel a job.
   *
   * <p>Even if a job was already finished, subsequent status requests will return that the job was
   * canceled.
   *
   * @param referenceId the job reference
   * @param statusResponse the response object
   */
  @RequestMapping(value = CANCEL_URL + "/{referenceId:\\S+}", method = RequestMethod.DELETE)
  public final void cancelPath(
      @Nonnull @PathVariable final String referenceId, final HttpServletResponse statusResponse) {
    cancel("default", referenceId, statusResponse);
  }

  /**
   * Cancel a job.
   *
   * <p>Even if a job was already finished, subsequent status requests will return that the job was
   * canceled.
   *
   * @param applicationId the application ID
   * @param referenceId the job reference
   * @param statusResponse the response object
   */
  public final void cancel(
      @Nonnull final String applicationId,
      @Nonnull final String referenceId,
      final HttpServletResponse statusResponse) {
    MDC.put(Processor.MDC_APPLICATION_ID_KEY, applicationId);
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
   *     generation.
   * @param createReportRequest the request object
   * @param createReportResponse the response object
   */
  @RequestMapping(value = "/{appId}" + REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
  public final void createReport(
      @Nonnull @PathVariable final String appId,
      @PathVariable final String format,
      @RequestBody final String requestData,
      final HttpServletRequest createReportRequest,
      final HttpServletResponse createReportResponse)
      throws NoSuchAppException {
    setNoCache(createReportResponse);
    String ref =
        createAndSubmitPrintJob(
            appId, format, requestData, createReportRequest, createReportResponse);
    if (ref == null) {
      error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    setContentType(createReportResponse);
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
   * @param inline whether to inline the
   * @param getReportResponse the response object
   */
  @RequestMapping(
      value = "/{appId}" + REPORT_URL + "/{referenceId:\\S+}",
      method = RequestMethod.GET)
  public final void getReportSpecificAppId(
      @Nonnull @PathVariable final String appId,
      @Nonnull @PathVariable final String referenceId,
      @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
      final HttpServletResponse getReportResponse)
      throws IOException, ServletException {
    getReport(appId, referenceId, inline, getReportResponse);
  }

  /**
   * To get the PDF created previously.
   *
   * @param referenceId the job reference
   * @param inline whether to inline the
   * @param getReportResponse the response object
   */
  @RequestMapping(value = REPORT_URL + "/{referenceId:\\S+}", method = RequestMethod.GET)
  public final void getReportPath(
      @Nonnull @PathVariable final String referenceId,
      @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
      final HttpServletResponse getReportResponse)
      throws IOException, ServletException {
    getReport("default", referenceId, inline, getReportResponse);
  }

  /**
   * To get the PDF created previously.
   *
   * @param applicationId the application ID
   * @param referenceId the job reference
   * @param inline whether to inline the
   * @param getReportResponse the response object
   */
  public final void getReport(
      @Nonnull final String applicationId,
      @Nonnull final String referenceId,
      final boolean inline,
      final HttpServletResponse getReportResponse)
      throws IOException, ServletException {
    MDC.put(Processor.MDC_APPLICATION_ID_KEY, applicationId);
    MDC.put(Processor.MDC_JOB_ID_KEY, referenceId);
    setNoCache(getReportResponse);
    loadReport(referenceId, getReportResponse, new VoidHandleReportLoadResult(inline));
  }

  /**
   * Add the print job to the job queue.
   *
   * @param format the format of the returned report
   * @param requestData a json formatted string with the request data required to perform the report
   *     generation.
   * @param createReportRequest the request object
   * @param createReportResponse the response object
   */
  @RequestMapping(value = REPORT_URL + ".{format:\\w+}", method = RequestMethod.POST)
  public final void createReport(
      @PathVariable final String format,
      @RequestBody final String requestData,
      final HttpServletRequest createReportRequest,
      final HttpServletResponse createReportResponse)
      throws NoSuchAppException {
    setNoCache(createReportResponse);
    PJsonObject spec = parseJson(requestData, createReportResponse);
    if (spec == null) {
      return;
    }
    final String appId = spec.optString(JSON_APP, DEFAULT_CONFIGURATION_FILE_KEY);
    if (appId == null) {
      throw new NoSuchAppException("No app specified");
    }
    createReport(appId, format, requestData, createReportRequest, createReportResponse);
  }

  /**
   * add the print job to the job queue.
   *
   * @param appId the id of the app to get the request for.
   * @param format the format of the returned report
   * @param requestData a json formatted string with the request data required to perform the report
   *     generation.
   * @param inline whether to inline the content
   * @param createReportRequest the request object
   * @param createReportResponse the response object
   */
  @RequestMapping(
      value = "/{appId}" + CREATE_AND_GET_URL + ".{format:\\w+}",
      method = RequestMethod.POST)
  public final void createReportAndGet(
      @Nonnull @PathVariable final String appId,
      @PathVariable final String format,
      @RequestBody final String requestData,
      @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
      final HttpServletRequest createReportRequest,
      final HttpServletResponse createReportResponse)
      throws IOException, ServletException, InterruptedException, NoSuchAppException {
    setNoCache(createReportResponse);

    String ref =
        createAndSubmitPrintJob(
            appId, format, requestData, createReportRequest, createReportResponse);
    if (ref == null) {
      error(createReportResponse, "Failed to create a print job", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    final BooleanHandleReportLoadResult handler = new BooleanHandleReportLoadResult(inline);
    boolean isDone = false;
    final long maxWaitTimeInMillis =
        TimeUnit.SECONDS.toMillis(this.maxCreateAndGetWaitTimeInSeconds);
    long startWaitTime = System.currentTimeMillis();
    while (!isDone && System.currentTimeMillis() - startWaitTime < maxWaitTimeInMillis) {
      TimeUnit.SECONDS.sleep(1);
      isDone = loadReport(ref, createReportResponse, handler);
    }
  }

  /**
   * add the print job to the job queue.
   *
   * @param format the format of the returned report
   * @param requestData a json formatted string with the request data required to perform the report
   *     generation.
   * @param inline whether to inline the content
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
    if (appId == null) {
      throw new NoSuchAppException("No app specified");
    }
    createReportAndGet(
        appId, format, requestData, inline, createReportRequest, createReportResponse);
  }

  /**
   * To get (in JSON) the information about the available formats and CO.
   *
   * @param listAppsResponse the response object
   */
  @RequestMapping(value = LIST_APPS_URL, method = RequestMethod.GET)
  public final void listAppIds(final HttpServletResponse listAppsResponse)
      throws ServletException, IOException {
    MDC.remove(Processor.MDC_APPLICATION_ID_KEY);
    MDC.remove(Processor.MDC_JOB_ID_KEY);
    setCache(listAppsResponse);
    Set<String> appIds = this.printerFactory.getAppIds();

    setContentType(listAppsResponse);
    try (PrintWriter writer = listAppsResponse.getWriter()) {
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
    }
  }

  /**
   * To get (in JSON) the information about the available formats and CO.
   *
   * @param pretty if true then pretty print the capabilities
   * @param request the request
   * @param capabilitiesResponse the response object
   */
  @RequestMapping(value = CAPABILITIES_URL, method = RequestMethod.GET)
  public final void getCapabilities(
      @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
      final HttpServletRequest request,
      final HttpServletResponse capabilitiesResponse)
      throws ServletException, IOException {
    getCapabilities(DEFAULT_CONFIGURATION_FILE_KEY, pretty, request, capabilitiesResponse);
  }

  /**
   * To get (in JSON) the information about the available formats and CO.
   *
   * @param appId the name of the "app" or in other words, a mapping to the configuration file for
   *     this request.
   * @param pretty if true then pretty print the capabilities
   * @param request the request
   * @param capabilitiesResponse the response object
   */
  @RequestMapping(value = "/{appId}" + CAPABILITIES_URL, method = RequestMethod.GET)
  public final void getCapabilities(
      @Nonnull @PathVariable final String appId,
      @RequestParam(value = "pretty", defaultValue = "false") final boolean pretty,
      final HttpServletRequest request,
      final HttpServletResponse capabilitiesResponse)
      throws ServletException, IOException {
    MDC.remove(Processor.MDC_APPLICATION_ID_KEY);
    MDC.remove(Processor.MDC_JOB_ID_KEY);
    setCache(capabilitiesResponse);
    MapPrinter printer;
    try {
      printer = this.printerFactory.create(appId);
      if (!checkReferer(request, printer)) {
        error(capabilitiesResponse, "Invalid referrer", HttpStatus.FORBIDDEN);
        return;
      }
    } catch (NoSuchAppException e) {
      error(capabilitiesResponse, e.getMessage(), HttpStatus.NOT_FOUND);
      return;
    }

    setContentType(capabilitiesResponse);

    final ByteArrayOutputStream prettyPrintBuffer = new ByteArrayOutputStream();

    try (Writer writer =
        pretty
            ? new OutputStreamWriter(prettyPrintBuffer, Constants.DEFAULT_CHARSET)
            : capabilitiesResponse.getWriter()) {
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
    }

    if (pretty) {
      final JSONObject jsonObject =
          new JSONObject(prettyPrintBuffer.toString(Constants.DEFAULT_CHARSET));
      capabilitiesResponse.getWriter().print(jsonObject.toString(JSON_INDENT_FACTOR));
    }
  }

  /**
   * Get a sample request for the app. An empty response may be returned if there is not example
   * request.
   *
   * @param request the request object
   * @param getExampleResponse the response object
   */
  @RequestMapping(value = EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
  public final void getExampleRequest(
      final HttpServletRequest request, final HttpServletResponse getExampleResponse)
      throws IOException {
    getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, request, getExampleResponse);
  }

  /**
   * Get a sample request for the app. An empty response may be returned if there is not example
   * request.
   *
   * @param appId the id of the app to get the request for.
   * @param request the request object
   * @param getExampleResponse the response object
   */
  @RequestMapping(value = "{appId}" + EXAMPLE_REQUEST_URL, method = RequestMethod.GET)
  public final void getExampleRequest(
      @Nonnull @PathVariable final String appId,
      final HttpServletRequest request,
      final HttpServletResponse getExampleResponse)
      throws IOException {
    MDC.remove(Processor.MDC_APPLICATION_ID_KEY);
    MDC.remove(Processor.MDC_JOB_ID_KEY);
    setCache(getExampleResponse);
    try {
      final MapPrinter mapPrinter = this.printerFactory.create(appId);
      if (!checkReferer(request, mapPrinter)) {
        error(getExampleResponse, "Invalid referrer", HttpStatus.FORBIDDEN);
        return;
      }
      final String requestDataPrefix = "requestData";
      final File[] children =
          mapPrinter
              .getConfiguration()
              .getDirectory()
              .listFiles(
                  (dir, name) -> name.startsWith(requestDataPrefix) && name.endsWith(".json"));
      if (children == null) {
        error(getExampleResponse, "Cannot find the config directory", HttpStatus.NOT_FOUND);
        return;
      }
      JSONObject allExamples = new JSONObject();

      for (File child : children) {
        if (child.isFile()) {
          String requestData = Files.readString(child.toPath(), Constants.DEFAULT_CHARSET);
          try {
            final JSONObject jsonObject = new JSONObject(requestData);
            jsonObject.remove(JSON_OUTPUT_FORMAT);
            jsonObject.remove(JSON_APP);
            requestData = jsonObject.toString(JSON_INDENT_FACTOR);

            setContentType(getExampleResponse);
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
            error(
                getExampleResponse,
                "Error translating object to json: " + e.getMessage(),
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
        error(
            getExampleResponse,
            "Error translating object to json: " + e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }

      try (PrintWriter writer = getExampleResponse.getWriter()) {
        writer.append(result);
      }
    } catch (NoSuchAppException e) {
      error(getExampleResponse, "No print app identified by: " + appId, HttpStatus.NOT_FOUND);
    }
  }

  /**
   * List the available fonts on the system.
   *
   * @param response the response object
   */
  @RequestMapping(value = FONTS_URL)
  public final void listAvailableFonts(final HttpServletResponse response) {
    MDC.remove(Processor.MDC_APPLICATION_ID_KEY);
    MDC.remove(Processor.MDC_JOB_ID_KEY);

    setContentType(response);
    try (PrintWriter writer = response.getWriter()) {
      JSONWriter json = new JSONWriter(writer);
      json.object();
      json.key(JSON_OUTPUT_JASPERREPORT_FONTS);
      json.array();

      final List<FontFamily> families =
          ExtensionsEnvironment.getExtensionsRegistry().getExtensions(FontFamily.class);
      for (FontFamily family : families) {
        json.value(family.getName());
      }
      json.endArray();

      json.key(JSON_OUTPUT_FONTS);
      json.array();
      for (String family : FontTools.FONT_FAMILIES) {
        json.object();
        json.key(JSON_OUTPUT_FONT_FAMILY).value(family);
        json.key(JSON_OUTPUT_FONTCONFIG);
        json.array();
        for (FontTools.FontConfigDescription description : FontTools.listFontConfigFonts(family)) {
          json.object();
          if (description.family != null) {
            json.key(JSON_OUTPUT_FONTCONFIG_FAMILIES);
            json.array();
            for (String fam : description.family) {
              json.value(fam);
            }
            json.endArray();
          }
          if (description.name != null) {
            json.key(JSON_OUTPUT_FONTCONFIG_NAME).value(description.name);
          }
          if (description.style != null) {
            json.key(JSON_OUTPUT_FONTCONFIG_STYLES);
            json.array();
            for (String style : description.style) {
              json.value(style);
            }
            json.endArray();
          }
          if (description.weight != 0) {
            json.key(JSON_OUTPUT_FONTCONFIG_WEIGHT).value(description.weight);
          }
          json.endObject();
        }
        json.endArray();
        json.endObject();
      }
      json.endArray();
      json.endObject();
    } catch (IOException e) {
      throw new PrintException("Failed to get writer from " + response, e);
    }
  }

  /**
   * Maximum time to wait for a createAndGet request to complete before returning an error.
   *
   * @param maxCreateAndGetWaitTimeInSeconds the maximum time in seconds to wait for a report to be
   *     generated.
   */
  public final void setMaxCreateAndGetWaitTimeInSeconds(
      final long maxCreateAndGetWaitTimeInSeconds) {
    this.maxCreateAndGetWaitTimeInSeconds = maxCreateAndGetWaitTimeInSeconds;
  }

  private void addDownloadLinkToJson(
      final HttpServletRequest httpServletRequest, final String ref, final JSONWriter json) {
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
      @Nonnull final String appId,
      final String format,
      final String requestDataRaw,
      final HttpServletRequest httpServletRequest,
      final HttpServletResponse httpServletResponse)
      throws NoSuchAppException {

    PJsonObject specJson = parseJson(requestDataRaw, httpServletResponse);
    if (specJson == null) {
      return null;
    }
    final String ref =
        maybeAddRequestId(
            UUID.randomUUID() + "@" + this.servletInfo.getServletId(), httpServletRequest);
    MDC.put(Processor.MDC_APPLICATION_ID_KEY, appId);
    MDC.put(Processor.MDC_JOB_ID_KEY, ref);
    LOGGER.debug("{} created Ref:{} for {}", httpServletRequest.getRequestURI(), ref, specJson);

    specJson.getInternalObj().remove(JSON_OUTPUT_FORMAT);
    specJson.getInternalObj().put(JSON_OUTPUT_FORMAT, format);
    specJson.getInternalObj().remove(JSON_APP);
    specJson.getInternalObj().put(JSON_APP, appId);
    final JSONObject requestHeaders = getHeaders(httpServletRequest);
    if (!requestHeaders.isEmpty()) {
      specJson
          .getInternalObj()
          .getJSONObject(JSON_ATTRIBUTES)
          .put(JSON_REQUEST_HEADERS, requestHeaders);
    }

    // check that we have authorization and configure the job, so it can only be accessed by users
    // with
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
      return null;
    }
    return ref;
  }

  private boolean checkReferer(final HttpServletRequest request, final MapPrinter mapPrinter) {
    final Configuration config = mapPrinter.getConfiguration();
    final UriMatchers allowedReferers = config.getAllowedReferersImpl();
    if (allowedReferers == null) {
      return true;
    }
    String referrer = request.getHeader("referer");
    if (referrer == null) {
      referrer = "http://localhost/";
    }
    try {
      return allowedReferers.matches(new URI(referrer), HttpMethod.resolve(request.getMethod()));
    } catch (SocketException
        | UnknownHostException
        | URISyntaxException
        | MalformedURLException e) {
      LOGGER.error("Referrer {} invalid", referrer, e);
      return false;
    }
  }

  private <R> R loadReport(
      final String referenceId,
      final HttpServletResponse httpServletResponse,
      final HandleReportLoadResult<R> handler)
      throws IOException, ServletException {
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

  private void setContentType(final HttpServletResponse statusResponse) {
    statusResponse.setContentType("application/json; charset=utf-8");
  }
}
