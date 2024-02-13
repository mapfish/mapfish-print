package org.mapfish.print.map;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.geotools.util.factory.Hints;
import org.mapfish.print.PrintException;

/** This class is an authority factory that allows defining custom EPSG codes. */
public final class CustomEPSGCodes extends FactoryUsingWKT {
  /** The path to the file containing the custom epsg codes. */
  public static final String CUSTOM_EPSG_CODES_FILE = "/epsg.properties";

  /** Constructor. */
  public CustomEPSGCodes() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param hints hints to pass to the framework on construction
   */
  public CustomEPSGCodes(final Hints hints) {
    super(hints);
  }

  /**
   * Returns the URL to the property file that contains CRS definitions.
   *
   * @return The URL to the epsg file containing custom EPSG codes
   */
  @Override
  protected URL getDefinitionsURL() {
    try {
      URL url = CustomEPSGCodes.class.getResource(CUSTOM_EPSG_CODES_FILE);
      // quickly test url
      try (InputStream stream = url.openStream()) {
        //noinspection ResultOfMethodCallIgnored
        stream.read();
      }
      return url;
    } catch (NullPointerException | IOException e) {
      throw new PrintException("Unable to load /epsg.properties file from root of classpath.", e);
    }
  }
}
