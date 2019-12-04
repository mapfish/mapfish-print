package org.mapfish.print.servlet.job.impl;

import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.PrintJobResult;

/**
 * A PrintJob implementation that write results to files.
 *
 */
public class FilePrintJob extends PrintJob {
    @Override
    protected PrintJobResult createResult(
            final String fileName, final String fileExtension, final String mimeType) {
        return new PrintJobResultImpl(getReportFile().toURI(), fileName, fileExtension, mimeType,
                                      getEntry().getReferenceId());
    }
}
