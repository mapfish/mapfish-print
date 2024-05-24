package org.mapfish.print.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/** This is used to load the utf-8 ResourceBundle files. */
public class ResourceBundleClassLoader extends ClassLoader {
  private final File configDir;

  /**
   * Construct.
   *
   * @param configDir the print application configuration directory.
   */
  public ResourceBundleClassLoader(final File configDir) {
    this.configDir = configDir;
  }

  @Override
  protected URL findResource(final String resource) {
    try {
      return new File(this.configDir, resource).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getResourceAsStream(final String resource) {
    ByteArrayOutputStream buffer;
    try (InputStream is = super.getResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource not found: " + resource);
      }
      buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[1024];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(buffer.toByteArray());
  }
}
