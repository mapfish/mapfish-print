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

package org.mapfish.print.servlet;

import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

/**
 * A {@link org.mapfish.print.MapPrinterFactory} that reads configuration from files and uses servlet's methods for resolving
 * the paths to the files.
 * <p/>
 * Created by Jesse on 3/18/14.
 */
public class ServletMapPrinterFactory implements MapPrinterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletMapPrinterFactory.class);

    @Autowired
    private ApplicationContext applicationContext;
    private final HashMap<String, Long> lastModifiedTimes = new HashMap<String, Long>();
    private Map<String, MapPrinter> printers = new HashMap<String, MapPrinter>();
    private long lastModified = 0L;
    private long defaultLastModified = 0L;

    @Override
    public final synchronized MapPrinter create(@Nullable final String app) {
        final ServletConfig servlet = this.applicationContext.getBean(Servlet.class).getServletConfig();
        String configPath = servlet.getInitParameter("config");
        if (configPath == null) {
            throw new RuntimeException(
                    "Missing configuration in web.xml 'web-app/servlet/init-param[param-name=config]' or " +
                    "'web-app/context-param[param-name=config]'");
        }

        MapPrinter printer = null;
        File configFile;
        if (app != null) {
            if (this.printers instanceof HashMap && this.printers.containsKey(app)) {
                printer = this.printers.get(app);
            } else {
                printer = null;
            }
            configFile = new File(app);
        } else {
            configFile = new File(configPath);
        }
        if (!configFile.isAbsolute()) {
            if (app != null) {
                if (app.toLowerCase().endsWith(".yaml")) {
                    configFile = new File(servlet.getServletContext().getRealPath(app));
                } else {
                    configFile = new File(servlet.getServletContext().getRealPath(app + ".yaml"));
                }
            } else {
                if (configPath.toLowerCase().endsWith(".yaml")) {
                    configFile = new File(servlet.getServletContext().getRealPath(configPath));
                } else {
                    configFile = new File(servlet.getServletContext().getRealPath(configPath + ".yaml"));
                }
                // debugPath += "config is absolute app DEFAULT\n";
            }
        }
        if (app != null) {
            if (this.lastModifiedTimes.containsKey(app)) {
                this.lastModified = this.lastModifiedTimes.get(app);
            } else {
                this.lastModified = 0L;
            }
        } else {
            this.lastModified = this.defaultLastModified;
        }

        boolean forceReload = false;
        if (printer != null && printer.getConfiguration().isReloadConfig()) {
            forceReload = true;
        }

        if (forceReload || (printer != null && configFile.lastModified() != this.lastModified)) {
            // file modified, reload it
            if (!forceReload) {
                LOGGER.info("Configuration file modified. Reloading...");
            }
            try {
                printer.close();

                // debugPath += "printer stopped, setting NULL\n";
            } catch (NullPointerException npe) {
                LOGGER.info("BaseMapServlet.java: printer was not stopped. This happens when a switch between applications happens.\n"
                            + npe);
            }

            printer = null;
            if (app != null) {
                LOGGER.info("Printer for " + app + " stopped");
                this.printers.put(app, null);
            }
        }

        if (printer == null) {
            this.lastModified = configFile.lastModified();
            try {
                LOGGER.info("Loading configuration file: " + configFile.getAbsolutePath());
                printer = this.applicationContext.getBean(MapPrinter.class);
                printer.setConfiguration(configFile);

                if (app != null) {
                    if (this.printers == null) {
                        this.printers = new HashMap<String, MapPrinter>();
                    }
                    this.printers.put(app, printer);
                    this.lastModifiedTimes.put(app, this.lastModified);
                } else {
                    this.defaultLastModified = this.lastModified; // need this for default
                    // application
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Cannot read configuration file: " + configPath, e);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file", e);
                throw new RuntimeException("Error occurred while reading configuration file '"
                                           + configFile + "': ", e);
            }
        }

        return printer;
    }
}
