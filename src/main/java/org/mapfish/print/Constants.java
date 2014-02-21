/*
 * Copyright (C) 2013  Camptocamp
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

/**
 * Strings used in configurations etc...
 * User: jeichar
 * Date: Sep 30, 2010
 * Time: 4:27:46 PM
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
    public interface ImagePlaceHolderConstants {
        String THROW = "throw";
        String DEFAULT = "default";
        String DEFAULT_ERROR_IMAGE = "default_error.png";
    }
}
