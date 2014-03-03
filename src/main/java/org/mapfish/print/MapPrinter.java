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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.servlet.queue.Queue;
import org.mapfish.print.servlet.registry.Registry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The main class for printing maps. Will parse the spec, create the PDF
 * document and generate it.
 *
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
    private List<OutputFormat> outputFormat;

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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
        throw new UnsupportedOperationException();
    }

    public void print(PJsonObject specJson, OutputStream out, Map<String, String> headers) {
    }

    public String getOutputFilename(String layout, String defaultName) {
        final String name = configuration.getOutputFilename(layout);
        return name == null ? defaultName : name;
    }
}
