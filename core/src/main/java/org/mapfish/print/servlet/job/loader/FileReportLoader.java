package org.mapfish.print.servlet.job.loader;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Loads reports from file uris.
 *
 */
public class FileReportLoader implements ReportLoader {
    @Override
    public final boolean accepts(final URI reportURI) {
        return reportURI.getScheme().equals("file");
    }

    @Override
    public final void loadReport(final URI reportURI, final OutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(reportURI.getPath())) {
            IOUtils.copy(in, out);
        }
    }
}
