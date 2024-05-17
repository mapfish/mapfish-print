package org.mapfish.print.servlet;

import java.io.IOException;
import java.net.URI;
import javax.servlet.http.HttpServletResponse;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.springframework.http.HttpStatus;

class BooleanHandleReportLoadResult extends HandleReportLoadResult<Boolean> {
  private final boolean isInlining;

  BooleanHandleReportLoadResult(final boolean inline) {
    isInlining = inline;
  }

  @Override
  public Boolean unknownReference(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    BaseMapServlet.error(
        httpServletResponse, "Print with ref=" + referenceId + " unknown", HttpStatus.NOT_FOUND);
    return true;
  }

  @Override
  public Boolean unsupportedLoader(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    BaseMapServlet.error(
        httpServletResponse,
        "Print with ref=" + referenceId + " can not be loaded",
        HttpStatus.NOT_FOUND);
    return true;
  }

  @Override
  public Boolean successfulPrint(
      final PrintJobStatus successfulPrintResult,
      final HttpServletResponse httpServletResponse,
      final URI reportURI,
      final ReportLoader loader)
      throws IOException {
    sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, isInlining);
    return true;
  }

  @Override
  public Boolean failedPrint(
      final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
    BaseMapServlet.error(
        httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
    return true;
  }

  @Override
  public Boolean printJobPending(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    return false;
  }
}
