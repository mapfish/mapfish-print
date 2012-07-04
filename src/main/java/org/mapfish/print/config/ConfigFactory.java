package org.mapfish.print.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.ho.yaml.CustomYamlConfig;
import org.ho.yaml.YamlConfig;
import org.mapfish.print.output.OutputFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Used by MapPrinter to create configuration objects.  Typically injected by spring
 *  
 * @author jeichar
 */
public class ConfigFactory {
	private OutputFactory outputFactory;
	
	@Autowired
	@Required
	public void setOutputFactory(OutputFactory outputFactory) {
		this.outputFactory = outputFactory;
	}
    /**
     * Create an instance out of the given file.
     */
    public Config fromYaml(File file) throws FileNotFoundException {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(file, Config.class);
        result.setOutputFactory(outputFactory);
        result.validate();
        return result;
    }

    public Config fromInputStream(InputStream instream) {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(instream, Config.class);
        result.setOutputFactory(outputFactory);
        result.validate();
        return result;
    }

    public Config fromString(String strConfig) {
        YamlConfig config = new CustomYamlConfig();
        Config result = config.loadType(strConfig, Config.class);
        result.setOutputFactory(outputFactory);
        result.validate();
        return result;
    }

}
