/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.servlet;

import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading)
 */
public abstract class BaseMapServlet extends HttpServlet {
    private static final long serialVersionUID = -6342262849725708850L;

    public static final Logger LOGGER = Logger.getLogger(BaseMapServlet.class);

    private MapPrinter printer = null;
    private long lastModified = 0L;

    /**
     * Builds a MapPrinter instance out of the file pointed by the servlet's
     * configuration. The location can be configured in two locations:
     * <ul>
     * <li>web-app/servlet/init-param[param-name=config] (top priority)
     * <li>web-app/context-param[param-name=config] (used only if the servlet has no config)
     * </ul>
     * <p/>
     * If the location is a relative path, it's taken from the servlet's root directory.
     */
    protected synchronized MapPrinter getMapPrinter() throws ServletException {
        String configPath = getInitParameter("config");
        if (configPath == null) {
            throw new ServletException("Missing configuration in web.xml 'web-app/servlet/init-param[param-name=config]' or 'web-app/context-param[param-name=config]'");
        }

        File configFile = new File(configPath);
        if (!configFile.isAbsolute()) {
            configFile = new File(getServletContext().getRealPath(configPath));
        }

        if (printer != null && configFile.lastModified() != lastModified) {
            //file modified, reload it
            LOGGER.info("Configuration file modified. Reloading...");
            printer.stop();
            printer = null;
        }

        if (printer == null) {
            lastModified = configFile.lastModified();
            try {
                LOGGER.info("Loading configuration file: " + configFile.getAbsolutePath());
                printer = new MapPrinter(configFile);
            } catch (FileNotFoundException e) {
                throw new ServletException("Cannot read configuration file: " + configPath, e);
            }
        }
        return printer;
    }

    @Override
    public synchronized void destroy() {
        if(printer != null) {
            printer.stop();
        }
        super.destroy();
    }
}