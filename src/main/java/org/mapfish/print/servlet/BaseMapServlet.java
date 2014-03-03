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

package org.mapfish.print.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mapfish.print.MapPrinter;
import org.mapfish.print.cli.Main;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading)
 */
public abstract class BaseMapServlet extends HttpServlet {
    private static final long serialVersionUID = -6342262849725708850L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);

    private Map<String, MapPrinter> printers = null;
    private long lastModified = 0L;
    private long defaultLastModified = 0L;
    private Map<String,Long> lastModifiedTimes = null;

    private volatile ApplicationContext context;

    /**
     * Builds a MapPrinter instance out of the file pointed by the servlet's
     * configuration. The location can be configured in two locations:
     * <ul>
     * <li>web-app/servlet/init-param[param-name=config] (top priority)
     * <li>web-app/context-param[param-name=config] (used only if the servlet
     * has no config)
     * </ul>
     * <p/>
     * If the location is a relative path, it's taken from the servlet's root
     * directory.
     *
     * @param servletContext
     */
    protected synchronized MapPrinter getMapPrinter(String app) throws ServletException {
        String configPath = getInitParameter("config");
        if (configPath == null) {
            throw new ServletException(
                    "Missing configuration in web.xml 'web-app/servlet/init-param[param-name=config]' or 'web-app/context-param[param-name=config]'");
        }
        // String debugPath = "";

        MapPrinter printer = null;
        File configFile = null;
        if (app != null) {
            if (lastModifiedTimes == null) {
                lastModifiedTimes = new HashMap<String, Long>();
                // debugPath += "new HashMap\n";
            }
            if (printers instanceof HashMap && printers.containsKey(app)) {
                printer = printers.get(app);
                // debugPath += "get printer from hashmap\n";
            } else {
                printer = null;
                // debugPath += "printer = null 1\n";
            }
            configFile = new File(app);
        } else {
            configFile = new File(configPath);
            // debugPath += "configFile = new ..., 1\n";
        }
        if (!configFile.isAbsolute()) {
            if (app != null) {
                // debugPath += "config is absolute app = "+app+"\n";
                if (app.toLowerCase().endsWith(".yaml")) {
                    configFile = new File(getServletContext().getRealPath(app));
                } else {
                    configFile = new File(getServletContext().getRealPath(app + ".yaml"));
                }
            } else {
                if (configPath.toLowerCase().endsWith(".yaml")) {
                    configFile = new File(getServletContext().getRealPath(configPath));
                } else {
                    configFile = new File(getServletContext().getRealPath(configPath + ".yaml"));
                }
                // debugPath += "config is absolute app DEFAULT\n";
            }
        }
        if (app != null) {
            if (lastModifiedTimes instanceof HashMap && lastModifiedTimes.containsKey(app)) {
                lastModified = lastModifiedTimes.get(app);
                // debugPath +=
                // "app = "+app+" lastModifieds has key and gotten: "+
                // lastModified +"\n";
            } else {
                lastModified = 0L;
                // debugPath +=
                // "app = "+app+" lastModifieds has NOT key and gotten: "+
                // lastModified +" (0L)\n";
            }
        } else {
            lastModified = defaultLastModified; // this is a fix for when
                                                // configuration files have
                                                // changed
            // debugPath +=
            // "app = NULL lastModifieds from defaultLastModified: "+
            // lastModified +"\n";
        }

        boolean forceReload = false;
        if (printer != null && printer.getConfiguration().isReloadConfig()) {
            forceReload = true;
        }

        if (forceReload || (printer != null && configFile.lastModified() != lastModified)) {
            // file modified, reload it
            if (!forceReload) {
                LOGGER.info("Configuration file modified. Reloading...");
            }
            try {
                printer.stop();

                // debugPath += "printer stopped, setting NULL\n";
            } catch (NullPointerException npe) {
                LOGGER.info("BaseMapServlet.java: printer was not stopped. This happens when a switch between applications happens.\n"
                        + npe);
            }

            printer = null;
            if (app != null) {
                LOGGER.info("Printer for " + app + " stopped");
                printers.put(app, null);
            }
        }

        if (printer == null) {
            lastModified = configFile.lastModified();
            try {
                LOGGER.info("Loading configuration file: " + configFile.getAbsolutePath());
                final Configuration configuration = context.getBean(ConfigurationFactory.class).getConfig(configFile);
                printer = getApplicationContext().getBean(MapPrinter.class);
                printer.setConfiguration(configuration);

                if (app != null) {
                    if (printers == null) {
                        printers = new HashMap<String, MapPrinter>();
                    }
                    printers.put(app, printer);
                    lastModifiedTimes.put(app, lastModified);
                } else {
                    defaultLastModified = lastModified; // need this for default
                                                        // application
                }
            } catch (FileNotFoundException e) {
                throw new ServletException("Cannot read configuration file: " + configPath, e);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file", e);
                throw new ServletException("Error occurred while reading configuration file '"
                        + configFile + "': " + e);
            }
        }

        return printer;
    }

    protected ApplicationContext getApplicationContext() {
        if (context == null) {
            synchronized (this) {
                if (context == null) {
                    context = WebApplicationContextUtils
                            .getWebApplicationContext(getServletContext());
                    if (context == null || context.getBean(MapPrinter.class) == null) {
                        String springConfig = System.getProperty("mapfish.print.springConfig");
                        if (springConfig != null) {
                            context = new FileSystemXmlApplicationContext(new String[] {
                                    "classpath:/" + Main.DEFAULT_SPRING_CONTEXT, springConfig });
                        } else {
                            context = new ClassPathXmlApplicationContext(
                                    Main.DEFAULT_SPRING_CONTEXT);
                        }
                    }
                }
            }
        }
        return context;
    }

    protected String getBaseUrl(HttpServletRequest httpServletRequest) {
        final String additionalPath = httpServletRequest.getPathInfo();
        String fullUrl = httpServletRequest.getParameter("url");
        if (fullUrl != null) {
            return fullUrl.replaceFirst(additionalPath + "$", "");
        } else {
            return httpServletRequest.getRequestURL().toString()
                    .replaceFirst(additionalPath + "$", "");
        }
    }

    protected static String cleanUpName(String original) {
        return original.replace(",", "").replaceAll("\\s+", "_");
    }

    protected static String findReplacement(String pattern, Date date) {
        if (pattern.toLowerCase().equals("date")) {
            return cleanUpName(DateFormat.getDateInstance().format(date));
        } else if (pattern.toLowerCase().equals("datetime")) {
            return cleanUpName(DateFormat.getDateTimeInstance().format(date));
        } else if (pattern.toLowerCase().equals("time")) {
            return cleanUpName(DateFormat.getTimeInstance().format(date));
        } else {
            try {
                return new SimpleDateFormat(pattern).format(date);
            } catch (Exception e) {
                LOGGER.error("Unable to format timestamp according to pattern: "+pattern, e);
                return "${"+pattern+"}";
            }
        }
    }

    /**
     * Send an error to the client with an exception
     */
    protected void error(HttpServletResponse httpServletResponse, Throwable e) {
        PrintWriter out = null;
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(500);
            out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            e.printStackTrace(out);

            LOGGER.error("Error while generating PDF", e);
        } catch (IOException ex) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Send an error to the client with a message
     */
    protected void error(HttpServletResponse httpServletResponse, String message, int code) {
        PrintWriter out = null;
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(code);
            out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            out.println(message);

            LOGGER.error("Error while generating PDF: " + message);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
