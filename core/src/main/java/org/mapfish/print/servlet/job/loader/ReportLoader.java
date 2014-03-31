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

package org.mapfish.print.servlet.job.loader;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Load a generated report from a supported URI.
 *
 * @author jesseeichar on 3/18/14.
 */
public interface ReportLoader {

    /**
     * Returns true if this loader can process the provided URI.
     *
     * @param reportURI the uri to test.
     */
    boolean accepts(URI reportURI);
    /**
     * Reads a report from the URI and writes it to the output stream.
     *
     * @param reportURI uri of the report.
     * @param out output stream to write to.
     */
    void loadReport(URI reportURI, OutputStream out) throws IOException;
}
