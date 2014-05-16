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

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

/**
 * A {@link org.mapfish.print.MapPrinterFactory} that reads configuration from files and uses servlet's methods for resolving
 * the paths to the files.
 * <p/>
 *
 * @author jesseeichar on 3/18/14.
 */
public class ServletMapPrinterFactory implements MapPrinterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletMapPrinterFactory.class);
    /**
     * The name of the default app.  This is always required to be one of the apps that are registered.
     */
    public static final String DEFAULT_CONFIGURATION_FILE_KEY = "default";

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ConfigurationFactory configurationFactory;

    @Autowired
    private ConfigFileLoaderManager configFileLoader;

    private Map<String, URI> configurationFiles = new HashMap<String, URI>();

    private final Map<String, MapPrinter> printers = Maps.newConcurrentMap();

    private final HashMap<String, Long> configurationFileLastModifiedTimes = new HashMap<String, Long>();

    @PostConstruct
    private void validateConfigurationFiles() {
        if (!this.configurationFiles.containsKey(DEFAULT_CONFIGURATION_FILE_KEY)) {
            throw new BeanCreationException(getClass().getName() + " requires that one of the configurationFiles is called '" +
                                            DEFAULT_CONFIGURATION_FILE_KEY + "'");
        }

        for (URI file : this.configurationFiles.values()) {
            Assert.isTrue(this.configFileLoader.isAccessible(file), file + " does not exist or is not accessible.");
        }
    }

    @Override
    public final synchronized MapPrinter create(@Nullable final String app) throws NoSuchAppException {
        String finalApp = app;
        if (app == null) {
            finalApp = DEFAULT_CONFIGURATION_FILE_KEY;
        }
        URI configFile = this.configurationFiles.get(finalApp);

        if (configFile == null) {
            throw new NoSuchAppException("There is no configurationFile registered in the " + getClass().getName() + " bean with the " +
                                         "id: " +
                                         "'" + configFile + "'");
        }

        final long lastModified;
        if (this.configurationFileLastModifiedTimes.containsKey(finalApp)) {
            lastModified = this.configurationFileLastModifiedTimes.get(finalApp);
        } else {
            lastModified = 0L;
        }

        MapPrinter printer = this.printers.get(finalApp);

        Optional<Long> configFileLastModified = this.configFileLoader.lastModified(configFile);
        if (configFileLastModified.isPresent() && configFileLastModified.get() > lastModified) {
            // file modified, reload it
            LOGGER.info("Configuration file modified. Reloading...");

            this.printers.remove(finalApp);
            printer = null;
        }

        if (printer == null) {
            if (configFileLastModified.isPresent()) {
                this.configurationFileLastModifiedTimes.put(finalApp, configFileLastModified.get());
            }

            try {
                LOGGER.info("Loading configuration file: " + configFile);
                printer = this.applicationContext.getBean(MapPrinter.class);
                byte[] bytes = this.configFileLoader.loadFile(configFile);
                printer.setConfiguration(configFile, bytes);

                this.printers.put(finalApp, printer);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file", e);
                throw new RuntimeException("Error occurred while reading configuration file '"
                                           + configFile + "': ", e);
            }
        }

        return printer;
    }

    @Override
    public final Set<String> getAppIds() {
        return this.configurationFiles.keySet();
    }

    /**
     * The setter for setting configuration file.  It will convert the value to a URI.
     *
     * @param configurationFiles the configuration file map.
     */
    public final void setConfigurationFiles(final Map<String, String> configurationFiles) throws URISyntaxException {
        this.configurationFiles.clear();
        for (Map.Entry<String, String> entry : configurationFiles.entrySet()) {
            if (!entry.getValue().contains(":/")) {
                // assume is a file
                this.configurationFiles.put(entry.getKey(), new File(entry.getValue()).toURI());
            } else {
                this.configurationFiles.put(entry.getKey(), new URI(entry.getValue()));
            }
        }

        if (this.configFileLoader != null) {
            this.validateConfigurationFiles();
        }
    }

    /**
     * Set a single directory that contains one or more subdirectories, each one that contains a config.yaml file will
     * be considered a print app.
     *
     * This can be called multiple times and each directory will add to the apps found in the other directories.  However
     * the appId is based on the directory names so if there are 2 directories with the same name the second will overwrite the
     * first encounter.
     *
     * @param directory the root directory containing the sub-app-directories.  This must resolve to a file with the
     */
    public final void setAppsRootDirectory(final String directory) throws URISyntaxException {

        final Iterable<File> children;

        if (!directory.contains(":/")) {
            children = Files.fileTreeTraverser().children(new File(directory));
        } else {
            final Optional<File> fileOptional = this.configFileLoader.toFile(new URI(directory));
            if (fileOptional.isPresent()) {
                children = Files.fileTreeTraverser().children(fileOptional.get());
            } else {
                throw new IllegalArgumentException(directory + " does not refer to a file on the current system.");
            }
        }
        for (File child : children) {
            final File configFile = new File(child, "config.yaml");
            if (configFile.exists()) {
                this.configurationFiles.put(child.getName(), configFile.toURI());
            }
        }
        if (this.configurationFiles.isEmpty()) {
            throw new IllegalArgumentException(directory + " is an emptry directory.  There must be at least one subdirectory " +
                                               "containing a config.yaml file");
        }

        // ensure there is a "default" app
        if (!this.configurationFiles.containsKey(DEFAULT_CONFIGURATION_FILE_KEY)) {
            final String next = this.configurationFiles.keySet().iterator().next();
            final URI uri = this.configurationFiles.remove(next);
            this.configurationFiles.put(DEFAULT_CONFIGURATION_FILE_KEY, uri);
        }
    }
}
