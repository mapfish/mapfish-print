package org.mapfish.print.config;

import java.io.File;
import java.net.URL;

/**
 * Configuration on how to store the reports until the user fetches them.
 */
public interface ReportStorage extends ConfigurationObject {
    /**
     * Save the report in the storage.
     *
     * @param ref The reference number.
     * @param filename The filename.
     * @param extension The file extension.
     * @param mimeType The mime type.
     * @param file The file containing the report.
     * @return The URL that can be used to fetch the result.
     */
    URL save(String ref, String filename, String extension, String mimeType, File file);
}
