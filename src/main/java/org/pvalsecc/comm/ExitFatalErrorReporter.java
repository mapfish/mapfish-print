/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.comm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExitFatalErrorReporter implements FatalErrorReporter {
    public static final Log LOGGER = LogFactory.getLog(ExitFatalErrorReporter.class);

    public void report(Throwable ex) {
        LOGGER.fatal("");
        LOGGER.fatal("The application has caught a fatal error and will exit.");
        LOGGER.fatal("");

        LOGGER.fatal(ex);

        LOGGER.fatal("");
        LOGGER.fatal("Bye bye!");
        LOGGER.fatal("");

        //noinspection CallToSystemExit
        System.exit(-1);
    }
}
