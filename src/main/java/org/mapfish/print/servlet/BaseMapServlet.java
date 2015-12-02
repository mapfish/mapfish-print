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

import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.ShellMapPrinter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading)
 */
public abstract class BaseMapServlet extends HttpServlet {
    private static final long serialVersionUID = -6342262849725708850L;

    public static final Logger LOGGER = Logger.getLogger(BaseMapServlet.class);

    private final Map<String, MapPrinter> printers = Maps.newHashMap();
    private final Map<String,Long> lastModifieds = Maps.newHashMap();

    private volatile ApplicationContext context;

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
    protected synchronized MapPrinter getMapPrinter(String app) throws ServletException {
        String configPath = System.getProperty("mapfish-print-config", getInitParameter("config"));
        if (configPath == null) {
            throw new ServletException("Missing configuration in web.xml 'web-app/servlet/init-param[param-name=config]' or 'web-app/context-param[param-name=config]'");
        }
        //String debugPath = "";

        if (app == null) {
            LOGGER.info("app is null, setting it as default configPath: " + configPath);
            app = configPath;
        }

        if (!app.toLowerCase().endsWith(".yaml")) {
            app = app + ".yaml";
        }

        File configFile = new File(app);

        if (!configFile.isAbsolute() || !configFile.exists()) {

            LOGGER.info("Attempting to locate app config file: '" + app + " in the webapplication.");
            String realPath = getServletContext().getRealPath(app);

            if (realPath != null) {
                configFile = new File(realPath);
            } else {
                LOGGER.info("Unable to find config file in web application using getRealPath.  Adding a / because that is often dropped");
                realPath = getServletContext().getRealPath("/" + app);
                configFile = new File(realPath);
            }
        }

        LOGGER.info("Loading app from: " + configFile);
        MapPrinter printer;
        final String configFileCanonicalPath;
        try {
            configFileCanonicalPath = configFile.getCanonicalPath();
            printer = printers.get(configFileCanonicalPath);
        } catch (IOException e) {
            throw new ServletException(e);
        }

        final long lastModified;
        if (lastModifieds.containsKey(app)) {
            lastModified = lastModifieds.get(app);
        } else {
            lastModified = 0L;
        }

        boolean forceReload = false;
        if (printer != null && (!printer.isRunning() || printer.getConfig().getReloadConfig())) {
            forceReload = true;
        }

        if (forceReload || (printer != null && (configFile.lastModified() != lastModified || !printer.isRunning()))) {
            //file modified, reload it
            if (!forceReload) {
                LOGGER.info("Configuration file modified. Reloading...");
            }
            try {
                printer.stop();

                //debugPath += "printer stopped, setting NULL\n";
            } catch (NullPointerException npe) {
                LOGGER.info("BaseMapServlet.java: printer was not stopped. This happens when a switch between applications happens.\n"+ npe);
            }

            printer = null;
            LOGGER.info("Printer for "+ app +" stopped");
            printers.put(configFileCanonicalPath, null);
        }

        if (printer == null) {
            //debugPath += "printer == null, lastModified from configFile = "+lastModified+"\n";
            try {
                LOGGER.info("Loading configuration file: " + configFile.getAbsolutePath());
                printer = getApplicationContext().getBean(MapPrinter.class).setYamlConfigFile(configFile);
                printers.put(configFileCanonicalPath, printer);
                lastModifieds.put(app,  configFile.lastModified());
            } catch (FileNotFoundException e) {
                throw new ServletException("Cannot read configuration file: " + configPath, e);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file", e);
                throw new ServletException("Error occurred while reading configuration file '" + configFile + "': " + e );
            }
        }
        if(printer != null) {
        	printer.start();
        }
        return printer;
    }

    private ApplicationContext getApplicationContext() {
        if (this.context == null) {
            synchronized (this) {
                if (this.context == null) {
                    this.context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                    if (this.context == null || context.getBean(MapPrinter.class) == null) {
                        String springConfig = System.getProperty("mapfish.print.springConfig");
                        if(springConfig != null) {
                            this.context = new FileSystemXmlApplicationContext(new String[]{"classpath:/"+ShellMapPrinter.DEFAULT_SPRING_CONTEXT, springConfig});
                        } else {
                            this.context = new ClassPathXmlApplicationContext(ShellMapPrinter.DEFAULT_SPRING_CONTEXT);
                        }
                    }
                }
            }
        }
        return this.context;
    }

}
