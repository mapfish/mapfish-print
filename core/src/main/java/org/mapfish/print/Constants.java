package org.mapfish.print;

import java.nio.charset.Charset;

/** Strings used in configurations etc... User: jeichar Date: Sep 30, 2010 Time: 4:27:46 PM */
public final class Constants {

  private Constants() {
    // not called
  }

  /** The layout tag in the json spec file. */
  public static final String JSON_LAYOUT_KEY = "layout";

  /** The output filename in the json spec file. */
  public static final String OUTPUT_FILENAME_KEY = "outputFilename";

  /**
   * The default encoding to use throughout the system. This can be set by setting the system
   * property:
   *
   * <p><em>mapfish.file.encoding</em>
   *
   * <p>before starting the JVM.
   */
  public static final String DEFAULT_ENCODING =
      System.getProperty("mapfish.file.encoding", "UTF-8");

  /** The default charset. Depends on {@link #DEFAULT_ENCODING}. */
  public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);

  /**
   * The DPI of a PDF according to the spec. Also the DPI used by old Openlayers versions (2.0 and
   * earlier).
   */
  public static final double PDF_DPI = 72.0;

  /** Used to convert inches in mm. */
  public static final double INCH_TO_MM = 25.4;

  /** The OGC standard pixel size in mm. */
  public static final double OGC_PIXEL_SIZE = 0.28;

  /** The OGC standard dpi. (About 90 dpi) */
  public static final double OGC_DPI = INCH_TO_MM / OGC_PIXEL_SIZE;

  /**
   * The amount of precision to use when comparing opacity levels. For example 0.0009 is considered
   * the same as 0.0 for opacity
   */
  public static final double OPACITY_PRECISION = 0.001;

  /** Style related constants. */
  public static final class Style {

    private Style() {
      // not called
    }

    public static final double POINT_SIZE = 10.0;

    /** Grid style constants. */
    public static final class Grid {

      private Grid() {
        // not called
      }

      /** The name of the grid feature type name. */
      public static final String NAME_LINES = "grid";

      /** The geometry attribute name. */
      public static final String ATT_GEOM = "geom";
    }

    /** Raster style constants. */
    public static final class Raster {

      private Raster() {
        // not called
      }

      /** The default style name for raster layers. */
      public static final String NAME = "raster";
    }

    /** Default Style for the Overview Map and Area of interest. */
    public static final class OverviewMap {

      private OverviewMap() {
        // not called
      }

      /** The default style name for the bbox rectangle in the overview map. */
      public static final String NAME = "overview-map";
    }

    public static final class PagingOverviewLayer {

      private PagingOverviewLayer() {
        // not called
      }

      /** The default style name for the bbox rectangle in the overview map. */
      public static final String NAME = "paging-overview-layer";
    }
  }
}
