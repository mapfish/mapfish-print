/*
 * Copyright (C) 2009  Camptocamp
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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.config.ConfigFactory;
import org.mapfish.print.output.OutputFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
//import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading)
 */
public abstract class BaseMapServlet extends HttpServlet {
    private static final long serialVersionUID = -6342262849725708850L;

    public static final Logger LOGGER = Logger.getLogger(BaseMapServlet.class);

    private MapPrinter printer = null;
    private Map<String, MapPrinter> printers = null;
    private long lastModified = 0L;
    private long defaultLastModified = 0L;
    private Map<String,Long> lastModifieds = null;

    /**
     * Builds a MapPrinter instance out of the file pointed by the servlet's
     * configuration. The location can be configured in two locations:
     * <ul>
     * <li>web-app/servlet/init-param[param-name=config] (top priority)
     * <li>web-app/context-param[param-name=config] (used only if the servlet has no config)
     * </ul>
     * <p/>
     * If the location is a relative path, it's taken from the servlet's root directory.
     * @param servletContext 
     */
    protected synchronized MapPrinter getMapPrinter(String app) throws ServletException {
        String configPath = getInitParameter("config");
        if (configPath == null) {
            throw new ServletException("Missing configuration in web.xml 'web-app/servlet/init-param[param-name=config]' or 'web-app/context-param[param-name=config]'");
        }
        //String debugPath = "";

        File configFile = null;
        if (app != null) {
        	if (lastModifieds == null) {
        		lastModifieds = new HashMap<String, Long>();
        		//debugPath += "new HashMap\n";
        	}
    		if (printers instanceof HashMap &&  printers.containsKey(app)) {
    			printer = printers.get(app);
    			//debugPath += "get printer from hashmap\n";
    		} else {
    			printer = null;
    			//debugPath += "printer = null 1\n";
    		}
       		configFile = new File(app +".yaml");
        } else {
        	configFile = new File(configPath);
        	//debugPath += "configFile = new ..., 1\n";
        }
        if (!configFile.isAbsolute()) {
        	if (app != null) {
        		//debugPath += "config is absolute app = "+app+"\n";
        		configFile = new File(getServletContext().getRealPath(app +".yaml"));
        	} else {
        		configFile = new File(getServletContext().getRealPath(configPath));
        		//debugPath += "config is absolute app DEFAULT\n";
        	}
        }
        if (app != null) {
        	if (lastModifieds instanceof HashMap && lastModifieds.containsKey(app)) {
        		lastModified = lastModifieds.get(app);
        		//debugPath += "app = "+app+" lastModifieds has key and gotten: "+ lastModified +"\n";
        	} else {
        		lastModified = 0L;
        		//debugPath += "app = "+app+" lastModifieds has NOT key and gotten: "+ lastModified +" (0L)\n";
        	}
        } else {
        	lastModified = defaultLastModified; // this is a fix for when configuration files have changed
        	//debugPath += "app = NULL lastModifieds from defaultLastModified: "+ lastModified +"\n";
        }
        
        boolean forceReload = false;
        if (printer != null && printer.getConfig().getReloadConfig()) {
            forceReload = true;
        }

        if (forceReload || (printer != null && configFile.lastModified() != lastModified)) {
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
            if (app != null) {
            	LOGGER.info("Printer for "+ app +" stopped");
            	printers.put(app, null);
            }
        }

        if (printer == null) {
            lastModified = configFile.lastModified();
            //debugPath += "printer == null, lastModified from configFile = "+lastModified+"\n";
            try {
                LOGGER.info("Loading configuration file: " + configFile.getAbsolutePath());
                //printer = WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(MapPrinter.class).setYamlConfigFile(configFile);
                /*
                 * The above line introduces a bug: When printing with two
                 * applications, say using app-a.yaml and app-b.yaml, 
                 * when printing app-a it works, then printing app-b, works.
                 * Then printing app-a again, it prints as configured with
                 * app-b.
                 * 
                 * We do not need DI if this class depends on MapPrinter 
                 * anyway which it does through the .setYamlConfigFile method
                 * and the call to MapPrinter.class. I guess in the future
                 * we could implement an interface and define that in the
                 * spring-application-context.xml file to take advantage of
                 * DI's power, but without an interface DI just introduces
                 * unnecessary complexity.
                 * 
                 * This class now depends on ConfigFactory and OutputFactory
                 * as well. This is not ideal. If we want to DI everything
                 * we should take advantage of interfaces. Otherwise I cannot
                 * see the point of DI, because it is mainly to be able to
                 * unit test things.
                 */
                printer = new MapPrinter();
                printer.setOutputFactory(WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(OutputFactory.class));
                printer.setConfigFactory(WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(ConfigFactory.class));
                printer.setYamlConfigFile(configFile);
                if (app != null) {
                	if (printers == null) {
                		printers = new HashMap<String, MapPrinter>();
                	}
                	printers.put(app, printer);
                	lastModifieds.put(app, lastModified);
                } else {
                	defaultLastModified = lastModified; // need this for default application
                }
            } catch (FileNotFoundException e) {
                throw new ServletException("Cannot read configuration file: " + configPath, e);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file", e);
            }
        }
        
        return printer;
    }

}