package org.mapfish.print.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called when a report is loaded to be sent to the user.
 *
 * @param <R> The return value
 */
abstract class HandleReportLoadResult<R> {
  private static final int FILENAME_MAX_LENGTH = 1000;
  private static final Pattern VARIABLE_PATTERN =
      Pattern.compile("\\$\\{(\\S{1," + FILENAME_MAX_LENGTH + "})}");
  private static final Logger LOGGER = LoggerFactory.getLogger(HandleReportLoadResult.class);

  /**
   * Called if the report reference is unknown.
   *
   * @param httpServletResponse response object
   * @param referenceId report id
   */
  abstract R unknownReference(HttpServletResponse httpServletResponse, String referenceId);

  /**
   * Called if no loader can be found for loading the report.
   *
   * @param httpServletResponse response object
   * @param referenceId report id
   */
  abstract R unsupportedLoader(HttpServletResponse httpServletResponse, String referenceId);

  /**
   * Called when a print succeeded.
   *
   * @param successfulPrintResult the result
   * @param httpServletResponse the http response
   * @param reportURI the uri to the report
   * @param loader the loader for loading the report.
   */
  abstract R successfulPrint(
      PrintJobStatus successfulPrintResult,
      HttpServletResponse httpServletResponse,
      URI reportURI,
      ReportLoader loader)
      throws IOException, ServletException;

  /**
   * Called when a print job failed.
   *
   * @param failedPrintJob the failed print job
   * @param httpServletResponse the object for writing response
   */
  abstract R failedPrint(PrintJobStatus failedPrintJob, HttpServletResponse httpServletResponse);

  /**
   * Called when the print job has not yet completed.
   *
   * @param httpServletResponse the object for writing response
   * @param referenceId report id
   */
  abstract R printJobPending(HttpServletResponse httpServletResponse, String referenceId);

  /**
   * Copy the PDF into the output stream.
   *
   * @param metadata the client request data
   * @param httpServletResponse the response object
   * @param reportLoader the object used for loading the report
   * @param reportURI the uri of the report
   * @param inline whether to inline the content
   */
  protected final void sendReportFile(
      final PrintJobStatus metadata,
      final HttpServletResponse httpServletResponse,
      final ReportLoader reportLoader,
      final URI reportURI,
      final boolean inline)
      throws IOException {

    try (OutputStream response = httpServletResponse.getOutputStream()) {
      httpServletResponse.setContentType(metadata.getResult().getMimeType());
      if (!inline) {
        String fileName = metadata.getResult().getFileName();
        Matcher matcher = getFileNameMatcher(fileName);
        while (matcher.find()) {
          final String variable = matcher.group(1);
          String replacement = findReplacement(variable, metadata.getCompletionDate());
          fileName = fileName.replace("${" + variable + "}", replacement);
          matcher = getFileNameMatcher(fileName);
        }

        fileName += "." + metadata.getResult().getFileExtension();
        httpServletResponse.setHeader(
            "Content-disposition", "attachment; filename=" + cleanUpName(fileName));
      }
      reportLoader.loadReport(reportURI, response);
    }
  }

  private static Matcher getFileNameMatcher(final String fileName) {
    if (fileName.length() > FILENAME_MAX_LENGTH) {
      throw new IllegalArgumentException("File name is too long");
    }
    return VARIABLE_PATTERN.matcher(fileName);
  }

  /**
   * Update a variable name with a date if the variable is detected as being a date.
   *
   * @param variableName the variable name.
   * @param date the date to replace the value with if the variable is a date variable.
   */
  private String findReplacement(final String variableName, final Date date) {
    if (variableName.equalsIgnoreCase("date")) {
      return cleanUpName(DateFormat.getDateInstance().format(date));
    } else if (variableName.equalsIgnoreCase("datetime")) {
      return cleanUpName(DateFormat.getDateTimeInstance().format(date));
    } else if (variableName.equalsIgnoreCase("time")) {
      return cleanUpName(DateFormat.getTimeInstance().format(date));
    } else {
      try {
        return new SimpleDateFormat(variableName).format(date);
      } catch (RuntimeException e) {
        LOGGER.error("Unable to format timestamp according to pattern: {}", variableName, e);
        return "${" + variableName + "}";
      }
    }
  }

  /**
   * Remove commas and whitespace from a string.
   *
   * @param original the starting string.
   */
  private String cleanUpName(final String original) {
    return original.replace(",", "").replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9-_.:+]", "_");
  }
}
