package org.mapfish.print.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigurationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
    
    private Map<String, String> yamlObjects;

    public Configuration getConfig(File configFile) throws IOException {
        Constructor constructor = new Constructor(Configuration.class);
        for (String tag : yamlObjects.keySet()) {
            try {
                @SuppressWarnings("unchecked")
                Class<Object> cl = (Class<Object>)Class.forName(yamlObjects.get(tag));
                constructor.addTypeDescription(new TypeDescription(cl, tag));
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error for a yaml class", e);
            }
        }
        Yaml yaml = new Yaml(constructor);

        FileInputStream in = null;
        try {
            in  = new FileInputStream(configFile);
            return (Configuration) yaml.load(new InputStreamReader(in, "UTF-8"));
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public Map<String, String> getYamlObjects() {
        return yamlObjects;
    }

    public void setYamlObjects(Map<String, String> yamlObjects) {
        this.yamlObjects = yamlObjects;
    }
}
