package org.mapfish.print.servlet.job.loader;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Load a generated report from a supported URI.
 */
public interface ReportLoader {

    /**
     * Returns true if this loader can process the provided URI.
     *
     * @param reportURI the uri to test.
     */
    boolean accepts(URI reportURI);

    /**
     * Reads a report from the URI and writes it to the output stream.
     *
     * @param reportURI uri of the report.
     * @param out output stream to write to.
     */
    void loadReport(URI reportURI, OutputStream out) throws IOException;
}
