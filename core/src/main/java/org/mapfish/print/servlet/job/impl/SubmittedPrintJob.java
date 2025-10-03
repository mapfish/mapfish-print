package org.mapfish.print.servlet.job.impl;

import java.util.concurrent.Future;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;

/**
 * Encapsulates a job that has been submitted to the JobManager.
 *
 * @param reportFuture the future for checking if the report is done and for getting the uri
 * @param entry the print job entry.
 */
public record SubmittedPrintJob(Future<PrintJobResult> reportFuture, PrintJobEntry entry) {}
