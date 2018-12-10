package org.mapfish.print.servlet.job.impl.hibernate;

import org.apache.commons.io.FileUtils;
import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.PrintJobResult;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A PrintJob implementation that write results to the database.
 * <p></p>
 */
public class HibernatePrintJob extends PrintJob {
    @Override
    protected final PrintJobResult createResult(
            final String fileName, final String fileExtension, final String mimeType)
            throws URISyntaxException, IOException {
        final byte[] data;
        if (getReportFile().exists()) {
            data = FileUtils.readFileToByteArray(getReportFile());
        } else {
            data = null;  // the report was sent.
        }

        final String referenceId = getEntry().getReferenceId();
        return new PrintJobResultExtImpl(new URI("hibernate:" + referenceId), fileName, fileExtension,
                                         mimeType, data, referenceId);
    }
}
