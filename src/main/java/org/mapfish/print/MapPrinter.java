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

package org.mapfish.print;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeSet;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.ConfigFactory;
import org.mapfish.print.output.OutputFactory;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.output.PrintParams;
import org.mapfish.print.utils.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.ByteBuffer;

/**
 * The main class for printing maps. Will parse the spec, create the PDF
 * document and generate it.
 *
 * This class should not be directly created but rather obtained from an application
 * context object so that all plugins and dependencies are correctly injected into it
 */
public class MapPrinter {
    /**
     * The parsed configuration file.
     *
     * This is a per instance property and while can be set by spring typically will be set by user of printer
     */
    private Config config;

    /**
     * The directory where the configuration file sits (used for ${configDir})
     *
     * This is a per instance property and while can be set by spring typically will be set by user of printer
     */
    private File configDir;

    /**
     * OutputFactory for the final output
     *
     * Injected by Spring
     */
    private OutputFactory outputFactory;
    /**
     * Factory for creating config objects
     *
     * Injected by Spring
     */
    private ConfigFactory configFactory;

    private volatile boolean fontsInitialized = false;

    static {
        //configure iText to use a higher precision for floats
        ByteBuffer.HIGH_PRECISION = true;
    }
    /**
     * OutputFactory for the final output
     *
     * Injected by Spring
     */
    @Autowired
    @Required
    public void setOutputFactory(OutputFactory outputFactory) {
        this.outputFactory = outputFactory;
    }
    /**
     * Factory for creating config objects
     *
     * Injected by Spring
     */
    @Autowired
    @Required
    public void setConfigFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }
    /**
     * Sets both the configuration by parsing the configFile and the configDir relative to the configFile
     * @param configFile
     * @throws FileNotFoundException
     * @return this
     */
    public MapPrinter setYamlConfigFile(File configFile) throws FileNotFoundException {
        this.config = configFactory.fromYaml(configFile);
        configDir = configFile.getParentFile();
        if (configDir == null) {
            try {
                configDir = new File(".").getCanonicalFile();
            } catch (IOException e) {
                configDir = new File(".");
            }
        }
        return this;
    }

    public MapPrinter setConfig(String strConfig) {
        this.config = configFactory.fromString(strConfig);
        return this;
    }

    public MapPrinter setConfig(InputStream inputConfig) {
        this.config =  configFactory.fromInputStream(inputConfig);
        return this;
    }

    public MapPrinter setConfigDir(String configDir) {
        this.configDir = new File(configDir);
        return this;
    }

    /**
     * Register the user specified fonts in iText.
     */
    private void initFonts() {
        if(!fontsInitialized) {
            synchronized (this) {
                if(!fontsInitialized) {
                    //we don't do that since it takes ages and that would hurt the perfs for
                    //the python controller:
                    //FontFactory.registerDirectories();

                    FontFactory.defaultEmbedding = true;

                    final TreeSet<String> fontPaths = config.getFonts();
                    if (fontPaths != null) {
                        for (String fontPath : fontPaths) {
                            fontPath = fontPath.replaceAll("\\$\\{configDir\\}", configDir.getPath());
                            File fontFile = new File(fontPath);
                            if (fontFile.isDirectory()) {
                                FontFactory.registerDirectory(fontPath, true);
                            } else {
                                FontFactory.register(fontPath);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate the PDF using the given spec.
     *
     * @return The context that was used for printing.
     * @throws InterruptedException
     */
    public RenderingContext print(PJsonObject jsonSpec, OutputStream outputStream, Map<String, String> headers) throws DocumentException, InterruptedException {
        initFonts();
        OutputFormat output = this.outputFactory.create(config, jsonSpec);

        PrintParams params = new PrintParams(config, configDir, jsonSpec, outputStream, headers);
        return output.print(params );

    }

    public static PJsonObject parseSpec(String spec) {
        final JSONObject jsonSpec;
        try {
            jsonSpec = new JSONObject(spec);
        } catch (JSONException e) {
            throw new RuntimeException("Cannot parse the spec file", e);
        }
        return new PJsonObject(jsonSpec, "spec");
    }

    /**
     * Use by /info.json to generate its returned content.
     */
    public void printClientConfig(JSONWriter json) throws JSONException {
        config.printClientConfig(json);
    }

    /**
     * Stop the thread pool or others.
     */
    @PreDestroy
    public void stop() {
        config.close();
    }

    public String getOutputFilename(String layout, String defaultName) {
        final String name = config.getOutputFilename(layout);
        return name == null ? defaultName : name;
    }

    public Config getConfig() {
        return config;
    }
    public OutputFormat getOutputFormat(PJsonObject jsonSpec) {
        return outputFactory.create(config, jsonSpec);
    }
}
