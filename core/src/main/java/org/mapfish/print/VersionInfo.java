package org.mapfish.print;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Allows to access version information (from the manifest).
 *
 * <p>Works only in a servlet context.
 */
public class VersionInfo {
  private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfo.class);

  private static final String MAPFISH_PRINT_VERSION = "Mapfish-Print-Version";
  private static final String GIT_VERSION = "Git-Revision";

  @Nullable private Attributes attrs;

  @Autowired private ServletContext servletContext;

  /** Output the version on the logs. */
  @PostConstruct
  public final void init() {
    this.attrs = getAttributes();
    if (this.attrs != null) {
      LOGGER.warn("Starting print version {} ({})", getVersion(), getGitHash());
    }
  }

  /**
   * @return The version
   */
  public final String getVersion() {
    return getValue(MAPFISH_PRINT_VERSION);
  }

  /**
   * @return The GIT hash
   */
  public final String getGitHash() {
    return getValue(GIT_VERSION);
  }

  private String getValue(final String name) {
    if (this.attrs != null) {
      final String value = this.attrs.getValue(name);
      return value != null ? value : "?";
    } else {
      return "?";
    }
  }

  private Attributes getAttributes() {
    if (this.servletContext == null
        || this.servletContext.getClass().getSimpleName().startsWith("Mock")) {
      return null;
    }
    try {
      final String path = this.servletContext.getRealPath("/META-INF/MANIFEST.MF");
      if (path != null) {
        try (InputStream stream = new FileInputStream(path)) {
          return new Manifest(stream).getMainAttributes();
        }
      }
    } catch (FileNotFoundException e) {
      LOGGER.warn("Cannot find servlet manifest");
    } catch (IOException e) {
      LOGGER.warn("Cannot read servlet manifest");
    }
    return null;
  }
}
