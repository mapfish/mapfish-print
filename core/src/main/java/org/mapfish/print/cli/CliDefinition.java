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

package org.mapfish.print.cli;

import com.sampullara.cli.Argument;

/**
 * A shell version of the MapPrinter. Can be used for testing or for calling
 * from other languages than Java.
 */
public final class CliDefinition {
    CliDefinition() {
        // this is intentionally empty
    }

    // CHECKSTYLE:OFF
    @Argument(description = "Filename for the configuration (templates&CO)", required = true)
    public String config = null;

    @Argument(description = "The location of the description of what has to be printed. By default, STDIN")
    public String spec = null;

    @Argument(description = "Used only if log4jConfig is not specified. 3 if you want everything, " +
                            "2 if you want the debug information (stacktraces are shown), 1 for infos and 0 for only warnings and errors")
    public String verbose = "1";

    @Argument(description = "The destination file. By default, Standard Out")
    public String output = null;

    @Argument(description = "Get the config for the client form. Doesn't generate a PDF")
    public boolean clientConfig = false;

    @Argument(description = "Spring configuration file to use in addition to the default.  This allows overriding certain values if " +
                            "desired")
    public String springConfig = null;

    @Argument(description = "If true then request data (spec) is in a old api request data (Mapfish v2 compatible).", alias = "v2")
    public boolean v2Api = false;

    @Argument(description = "Print all the commandline options.", alias = "?")
    public boolean help = false;
}
