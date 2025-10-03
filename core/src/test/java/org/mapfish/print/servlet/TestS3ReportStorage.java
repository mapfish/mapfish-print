package org.mapfish.print.servlet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.mapfish.print.config.S3ReportStorage;

public class TestS3ReportStorage extends S3ReportStorage {

  @Override
  public URL save(
      final String ref,
      final String filename,
      final String extension,
      final String mimeType,
      final File file) {
    try {
      String spec =
          String.format("https://example.com/%s/%s", getBucket(), getKey(ref, filename, extension));
      return URL.of(new URI(spec), null);
    } catch (MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
