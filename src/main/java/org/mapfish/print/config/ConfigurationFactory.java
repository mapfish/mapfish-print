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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Strategy/plug-in for loading {@link Configuration} objects.
 *  
 * @author Jesse
 *
 */
public class ConfigurationFactory {
    @Autowired
    private Map<String, ConfigurationObject> yamlObjects;
    private Yaml yaml;

    /**
     * initialize this factory.  Called by spring after construction.
     */
    @PostConstruct
	public final void init() {
        Constructor constructor = new Constructor(Configuration.class);
        for (Map.Entry<String, ConfigurationObject> entry : this.yamlObjects.entrySet()) {
            constructor.addTypeDescription(new TypeDescription(entry.getValue().getClass(), entry.getKey()));
        }
        this.yaml = new Yaml(constructor);
    }

    /**
     * Create a configuration object from a config file.
     * 
     * @param configFile the file to read the configuration from.
     */
    public final Configuration getConfig(final File configFile) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(configFile);
            return (Configuration) this.yaml.load(new InputStreamReader(in, "UTF-8"));
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
