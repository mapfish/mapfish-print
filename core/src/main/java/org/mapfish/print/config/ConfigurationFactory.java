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

package org.mapfish.print.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.annotation.PostConstruct;

/**
 * Strategy/plug-in for loading {@link Configuration} objects.
 *
 * @author Jesse
 */
public class ConfigurationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
 @Autowired
 private ConfigurableApplicationContext context;
    private Yaml yaml;
    private boolean doValidation = true;


    /**
     * initialize this factory.  Called by spring after construction.
     */
    @PostConstruct
    public final void init() {
        MapfishPrintConstructor constructor = new MapfishPrintConstructor(this.context);
        this.yaml = new Yaml(constructor);
    }

    /**
     * Create a configuration object from a config file.
     *
     * @param configFile the file to read the configuration from.
     */
    @VisibleForTesting
    public final Configuration getConfig(final File configFile) throws IOException {
        Closer closer = Closer.create();
        try {
            FileInputStream in = closer.register(new FileInputStream(configFile));
           return getConfig(configFile, in);
        } finally {
            closer.close();
        }
    }
    /**
     * Create a configuration object from a config file.
     *
     * @param configFile the file that contains the configuration data.
     * @param configData the config file data
     */
    public final Configuration getConfig(final File configFile, final InputStream configData) throws IOException {
        final Configuration configuration = this.context.getBean(Configuration.class);
        configuration.setConfigurationFile(configFile);
        MapfishPrintConstructor.setConfigurationUnderConstruction(configuration);

        final Configuration config = (Configuration) this.yaml.load(new InputStreamReader(configData, "UTF-8"));
        if (this.doValidation) {
            final List<Throwable> validate = config.validate();
            if (!validate.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (Throwable throwable : validate) {
                    errors.append("\n\t* ").append(throwable.getMessage());
                    LOGGER.error("Configuration Error found: ", throwable);
                }

                throw new Error(errors.toString());
            }
        }
        return config;
    }

    /**
     * If doValidation is true then the Configuration object will be validated after loading.  However for some
     * tests we don't want this so this method allows it to be set to false for tests.
     *
     * By default it is true so only tests should modify this.
     *
     * @param doValidation the new validation value.
     */
    public final void setDoValidation(final boolean doValidation) {
        this.doValidation = doValidation;
    }
}
