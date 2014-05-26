package org.mapfish.print.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.codahale.metrics.MetricRegistry;
import org.ho.yaml.CustomYamlConfig;
import org.ho.yaml.YamlConfig;
import org.mapfish.print.ThreadResources;
import org.mapfish.print.map.readers.MapReaderFactoryFinder;
import org.mapfish.print.output.OutputFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Used by MapPrinter to create configuration objects.  Typically injected by spring
 *
 * @author jeichar
 */
public class ConfigFactory {
    @Autowired
    private OutputFactory outputFactoryFinder;
    @Autowired
    private MapReaderFactoryFinder mapReaderFactoryFinder;
    @Autowired
    private ThreadResources threadResources;
    @Autowired
    private MetricRegistry metricRegistry;


    public ConfigFactory() {
    }

    public ConfigFactory(ThreadResources threadResources) {
        // this is mainly for testing.  normally a factory should be part of spring configuration.
        this.threadResources = threadResources;
    }



    /**
     * Create an instance out of the given file.
     */
    public Config fromYaml(File file) throws FileNotFoundException {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(file, Config.class);
        result.setOutputFactory(outputFactoryFinder);
        result.setMapReaderFactoryFinder(mapReaderFactoryFinder);
        result.setThreadResources(this.threadResources);
        result.setMetricRegistry(this.metricRegistry);
        result.validate();
        return result;
    }

    public Config fromInputStream(InputStream instream) {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(instream, Config.class);
        result.setOutputFactory(outputFactoryFinder);
        result.setMapReaderFactoryFinder(mapReaderFactoryFinder);
        result.setThreadResources(this.threadResources);
        result.setMetricRegistry(this.metricRegistry);
        result.validate();
        return result;
    }

    public Config fromString(String strConfig) {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(strConfig, Config.class);
        result.setOutputFactory(outputFactoryFinder);
        result.setMapReaderFactoryFinder(mapReaderFactoryFinder);
        result.setThreadResources(this.threadResources);
        result.setMetricRegistry(this.metricRegistry);
        result.validate();
        return result;
    }

}
