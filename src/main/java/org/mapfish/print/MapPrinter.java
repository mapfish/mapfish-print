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

package org.mapfish.print;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.servlet.queue.Queue;
import org.mapfish.print.servlet.registry.Registry;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * The main class for printing maps. Will parse the spec, create the PDF
 * document and generate it.
 * <p/>
 * This class should not be directly created but rather obtained from an application
 * context object so that all plugins and dependencies are correctly injected into it
 */
public class MapPrinter {

    private Configuration configuration;
    @Autowired
    private Queue queue;
    @Autowired
    private Registry registry;
    @Autowired
    private Map<String, OutputFormat> outputFormat;
    @Autowired
    private ConfigurationFactory configurationFactory;
    private File configFile;

    public Queue getQueue() {
        return queue;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setConfiguration(File configFile) throws IOException {
        this.configFile = configFile;
        this.configuration = configurationFactory.getConfig(configFile);
    }

    public Configuration getConfiguration() {
        return configuration;
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
     *
     * @param json the writer for outputting the config specification
     */
    public void printClientConfig(JSONWriter json) throws JSONException {
        configuration.printClientConfig(json);
    }

    public void stop() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public OutputFormat getOutputFormat(PJsonObject specJson) {
        String format = specJson.getString("outputFormat");
        return outputFormat.get(format);
    }

    public void print(PJsonObject specJson, OutputStream out, Map<String, String> headers) throws Exception {
        // TODO use queue etc..
        final OutputFormat format = getOutputFormat(specJson);
        format.print(specJson, getConfiguration(), configFile.getParentFile(), out);
    }

    public String getOutputFilename(String layout, String defaultName) {
        final String name = configuration.getOutputFilename(layout);
        return name == null ? defaultName : name;
    }
}
