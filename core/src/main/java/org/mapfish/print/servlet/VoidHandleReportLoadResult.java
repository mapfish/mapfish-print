package org.mapfish.print.servlet;

import java.io.IOException;
import java.net.URI;
import javax.servlet.http.HttpServletResponse;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.springframework.http.HttpStatus;

class VoidHandleReportLoadResult extends HandleReportLoadResult<Void> {

  private final boolean isInlining;

  VoidHandleReportLoadResult(final boolean inline) {
    this.isInlining = inline;
  }

  @Override
  public Void unknownReference(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    BaseMapServlet.error(
        httpServletResponse,
        "Error getting print with ref=" + referenceId + ": unknown reference",
        HttpStatus.NOT_FOUND);
    return null;
  }

  @Override
  public Void unsupportedLoader(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    BaseMapServlet.error(
        httpServletResponse,
        "Error getting print with ref=" + referenceId + " can not be loaded",
        HttpStatus.NOT_FOUND);
    return null;
  }

  @Override
  public Void successfulPrint(
      final PrintJobStatus successfulPrintResult,
      final HttpServletResponse httpServletResponse,
      final URI reportURI,
      final ReportLoader loader)
      throws IOException {
    sendReportFile(successfulPrintResult, httpServletResponse, loader, reportURI, isInlining);
    return null;
  }

  @Override
  public Void failedPrint(
      final PrintJobStatus failedPrintJob, final HttpServletResponse httpServletResponse) {
    BaseMapServlet.error(
        httpServletResponse, failedPrintJob.getError(), HttpStatus.INTERNAL_SERVER_ERROR);
    return null;
  }

  @Override
  public Void printJobPending(
      final HttpServletResponse httpServletResponse, final String referenceId) {
    BaseMapServlet.error(
        httpServletResponse, "Report has not yet completed processing", HttpStatus.ACCEPTED);
    return null;
  }
}
