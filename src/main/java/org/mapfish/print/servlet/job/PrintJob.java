package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.servlet.BaseMapServlet;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The information for printing a report.
 *
 * @author Jesse
 */
public abstract class PrintJob implements Callable<CompletedPrintJob> {
    private static final String JSON_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintJob.class);

    private String referenceId;
    private PJsonObject requestData;
    private HttpHeaders headers;

    @Autowired
    private JobManager jobManager;
    @Autowired
    private MapPrinterFactory mapPrinterFactory;
    @Autowired
    private MetricRegistry metricRegistry;

    /**
     * Get the reference id of the job so it can be looked up again later.
     */
    public final String getReferenceId() {
        return this.referenceId;
    }

    /**
     * Set the reference id of the job so it can be looked up again later.
     *
     * @param referenceId the referenceId
     */
    public final void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Set the data from the client making the request.
     *
     * @param requestData the json data
     */
    public final void setRequestData(final PJsonObject requestData) {
        this.requestData = requestData;
    }

    /**
     * Set The request headers to use when making requests.
     *
     * @param headers The request headers to use when making requests.
     */
    public final void setHeaders(final HttpHeaders headers) {
        this.headers = headers;
    }

    /**
     * Open an OutputStream and execute the function using the OutputStream.
     *
     * @param function the function to execute
     * @return the
     */
    protected abstract URI withOpenOutputStream(PrintAction function) throws Throwable;

    @Override
    public final CompletedPrintJob call() throws Exception {
        long duration = new Date().getTime() - this.jobManager.timeSinceLastStatusCheck(this.referenceId);

        if (duration > TimeUnit.MINUTES.toMillis(1)) {
            throw new TimeoutException("Request is cancelled because the client has not requested the status within the last 1 minute");
        }

        final String appId = PrintJob.this.requestData.getString(MapPrinterServlet.JSON_APP);

        final PJsonObject spec = PrintJob.this.requestData.getJSONObject(MapPrinterServlet.JSON_SPEC);
        final String fileName = getFileName(spec.optString(Constants.OUTPUT_FILENAME_KEY), new Date());

        Timer.Context timer = this.metricRegistry.timer(getClass().getName() + " call()").time();
        try {
            URI reportURI = withOpenOutputStream(new PrintAction() {
                @Override
                public void run(final OutputStream outputStream) throws Throwable {
                    final MapPrinter mapPrinter = PrintJob.this.mapPrinterFactory.create(appId);
                    doCreatePDFFile(PrintJob.this.requestData, mapPrinter, outputStream);
                }
            });

            this.metricRegistry.counter(getClass().getName() + "success").inc();
            return new SuccessfulPrintJob(this.referenceId, reportURI, appId, fileName);
        } catch (Throwable e) {
            this.metricRegistry.counter(getClass().getName() + "failure").inc();
            return new FailedPrintJob(this.referenceId, appId, fileName, e.getMessage());
        } finally {
            final long stop = timer.stop();
            LOGGER.debug("Print Job completed in " + stop + "ms");
        }
    }

    /**
     * Create the file name of the resulting PDF file (the name the client wants).
     * Will replace the ${date}, ${datetime}, ${time}, ${simple-date-format-string} with
     * the correctly formatted date string.
     *
     * @param fileName the filename.
     * @param date     the date of the print job request.
     */
    private static String getFileName(final String fileName, final Date date) {
        Matcher matcher = Pattern.compile("\\$\\{(.+?)\\}").matcher(fileName);
        Map<String, String> replacements = new HashMap<String, String>();
        while (matcher.find()) {
            String pattern = matcher.group(1);
            String key = "${" + pattern + "}";
            replacements.put(key, BaseMapServlet.findReplacement(pattern, date));
        }
        String result = fileName;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }


    private void doCreatePDFFile(final PJsonObject job, final MapPrinter mapPrinter, final OutputStream out)
            throws Exception {

        Map<String, String> headerMap = new HashMap<String, String>();
        TreeSet<String> configHeaders = mapPrinter.getConfiguration().getHeaders();
        if (configHeaders == null) {
            configHeaders = new TreeSet<String>();
            configHeaders.add("Referrer");
            configHeaders.add("Cookie");
        }
        PJsonObject jobHeaders = job.getJSONObject(JSON_HEADERS);
        for (String header : configHeaders) {
            String headerValue = jobHeaders.optString(header);
            if (headerValue != null) {
                headerMap.put(header, headerValue);
            }
        }

        final PJsonObject spec = job.getJSONObject(MapPrinterServlet.JSON_SPEC);
        mapPrinter.print(spec, out, headerMap);
    }


    /**
     * Interface encapsulating the code to run with the open output stream.
     */
    protected interface PrintAction {
        /**
         * Execute the action.
         *
         * @param outputStream the output stream to write the report to.
         */
        void run(OutputStream outputStream) throws Throwable;
    }
}
