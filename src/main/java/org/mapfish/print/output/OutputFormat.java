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

package org.mapfish.print.output;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.json.PJsonObject;

import java.io.File;
import java.io.OutputStream;

/**
 * Interface for exporting the generated PDF from MapPrinter.
 * <p/>
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 1:49:41 PM
 */
public interface OutputFormat {
    /**
     * The content type of the output.
     */
    String getContentType();

    /**
     * The file suffix to use when writing to a file.
     */
    String getFileSuffix();

    /**
     * Performs the print and writes to the report in the correct format to the outputStream.
     *
     * @param spec the data from the client, required for writing.
     * @param config the configuration object representing the server side configuration.
     * @param configDir the directory that contains the configuration, used for resolving resources like images etc...
     * @param outputStream the stream to write the result to
     */
    void print(PJsonObject spec, Configuration config, File configDir, OutputStream outputStream) throws Exception;
}
