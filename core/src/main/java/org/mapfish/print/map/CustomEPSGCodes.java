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
package org.mapfish.print.map;

import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * This class is an authority factory that allows defining custom EPSG codes.
 */
public final class CustomEPSGCodes extends FactoryUsingWKT {
    /**
     * The path to the file containing the custom epsg codes.
     */
    public static final String CUSTOM_EPSG_CODES_FILE = "/epsg.properties";

    /**
     * Constructor.
     */
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
     * Returns the URL to the property file that contains CRS definitions.     *
     * @return The URL to the epsg file containing custom EPSG codes
     */
    @Override
    protected URL getDefinitionsURL() {
        InputStream stream = null;
        try {
            URL url = CustomEPSGCodes.class.getResource(CUSTOM_EPSG_CODES_FILE);
            // quickly test url
            stream = url.openStream();
            stream.read();
            stream.close();
            return url;
        } catch (Throwable e) {
            throw new AssertionError("Unable to load /epsg.properties file from root of classpath.");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
