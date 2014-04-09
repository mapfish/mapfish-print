/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print;

import java.nio.charset.Charset;

/**
 * Strings used in configurations etc...
 * User: jeichar
 * Date: Sep 30, 2010
 * Time: 4:27:46 PM
 * <p/>
 * CSOFF:MagicNumber
 * CSOFF:RequireThis
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
     * <p/>
     * <em>mapfish.file.encoding</em>
     * <p/>
     * before starting the JVM.
     */
    String DEFAULT_ENCODING = System.getProperty("mapfish.file.encoding", "UTF-8");
    /**
     * The default charset.  Depends on {@link #DEFAULT_ENCODING}.
     */
    Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);
    /**
     * The DPI of a PDF according to the spec.  Also the DPI used by old Openlayers versions (2.0 and earlier).
     */
    double PDF_DPI = 72.0;

    /**
     * The default style name for raster layers.
     */
    String RASTER_STYLE_NAME = "raster";
    /**
     * The OGC standard dpi. (About 90 dpi)
     */
    double OGC_DPI = 25.4 / 0.28;
}
