package org.mapfish.print;

import java.nio.charset.Charset;

/**
 * Strings used in configurations etc... User: jeichar Date: Sep 30, 2010 Time: 4:27:46 PM
 *
 */
public interface Constants {
    /**
     * The layout tag in the json spec file.
     */
    String JSON_LAYOUT_KEY = "layout";

    /**
     * The output filename in the json spec file.
     */
    String OUTPUT_FILENAME_KEY = "outputFilename";

    /**
     * The default encoding to use throughout the system.  This can be set by setting the system property:
     *
     * <em>mapfish.file.encoding</em>
     *
     * before starting the JVM.
     */
    String DEFAULT_ENCODING = System.getProperty("mapfish.file.encoding", "UTF-8");

    /**
     * The default charset.  Depends on {@link #DEFAULT_ENCODING}.
     */
    Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);

    /**
     * The DPI of a PDF according to the spec.  Also the DPI used by old Openlayers versions (2.0 and
     * earlier).
     */
    double PDF_DPI = 72.0;

    /**
     * Used to convert inches in mm.
     */
    double INCH_TO_MM = 25.4;

    /**
     * The OGC standard pixel size in mm.
     */
    double OGC_PIXEL_SIZE = 0.28;

    /**
     * The OGC standard dpi. (About 90 dpi)
     */
    double OGC_DPI = INCH_TO_MM / OGC_PIXEL_SIZE;

    /**
     * The amount of precision to use when comparing opacity levels.  For example 0.0009 is considered the
     * same as 0.0 for opacity
     */
    double OPACITY_PRECISION = 0.001;

    /**
     * Style related constants.
     */
    interface Style {
        double POINT_SIZE = 10.0;

        /**
         * Grid style constants.
         */
        interface Grid {

            /**
             * The name of the grid feature type name.
             */
            String NAME_LINES = "grid";
            /**
             * The geometry attribute name.
             */
            String ATT_GEOM = "geom";
        }

        /**
         * Raster style constants.
         */
        interface Raster {

            /**
             * The default style name for raster layers.
             */
            String NAME = "raster";
        }

        /**
         * Default Style for the Overview Map and Area of interest.
         */
        interface OverviewMap {

            /**
             * The default style name for the bbox rectangle in the overview map.
             */
            String NAME = "overview-map";
        }
    }
}
