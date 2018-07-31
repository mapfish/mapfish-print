package org.mapfish.print.servlet.job;

import java.net.URI;

/**
 * Print Job Result.
 */
public interface PrintJobResult {

    /**
     * Get the report URI.
     */
    URI getReportURI();

    /**
     * Get the report mime type.
     */
    String getMimeType();

    /**
     * Get the file extension.
     */
    String getFileExtension();

    /**
     * Get the file name.
     */
    String getFileName();

    /**
     * Get the report URI as String.
     */
    String getReportURIString();
}
