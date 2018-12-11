package org.mapfish.print.servlet;

import org.mapfish.print.config.S3ReportStorage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class TestS3ReportStorage extends S3ReportStorage {

    @Override
    public URL save(
            final String ref, final String filename, final String extension, final String mimeType,
            final File file) {
        try {
            return new URL(String.format("https://example.com/%s/%s", getBucket(),
                                         getKey(ref, filename, extension)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
