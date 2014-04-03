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
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Closeable;
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
public class MapPrinter implements Closeable {

    private Configuration configuration;
    @Autowired
    private Map<String, OutputFormat> outputFormat;
    @Autowired
    private ConfigurationFactory configurationFactory;
    private File configFile;

    /**
     * Set the configuration file and update the configuration for this printer.
     *
     * @param newConfigFile the file containing the new configuration.
     */
    public final void setConfiguration(final File newConfigFile) throws IOException {
        this.configFile = newConfigFile;
        this.configuration = this.configurationFactory.getConfig(newConfigFile);
    }

    public final Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Use by /info.json to generate its returned content.
     * @param json the writer for outputting the config specification
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        this.configuration.printClientConfig(json);
    }

    /**
     * Parse the JSON string and return the object.  The string is expected to be the JSON print data from the client.
     *
     * @param spec the JSON formatted string.
     * @return The encapsulated JSON object
     */
    public static PJsonObject parseSpec(final String spec) {
        final JSONObject jsonSpec;
        try {
            jsonSpec = new JSONObject(spec);
        } catch (JSONException e) {
            throw new RuntimeException("Cannot parse the spec file", e);
        }
        return new PJsonObject(jsonSpec, "spec");
    }

    /**
     * Shut down any resources that the mapprinter might have started like HTTP connections or open files, database connections, etc...
     */
    @Override
    public final void close() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * Get the object responsible for printing to the correct output format.
     *
     * @param specJson the request json from the client
     */
    public final OutputFormat getOutputFormat(final PJsonObject specJson) {
        String format = specJson.getString("outputFormat");
        return this.outputFormat.get(format + "OutputFormat");
    }

    /**
     * Start a print.
     *
     * @param specJson the client json request.
     * @param out the stream to write to.
     * @param headers the headers passed from client.
     */
    public final void print(final PJsonObject specJson, final OutputStream out, final Map<String, String> headers)
            throws Exception {
        // TODO use queue etc..
        final OutputFormat format = getOutputFormat(specJson);
        format.print(specJson, getConfiguration(), this.configFile.getParentFile(), out);
    }

    /**
     * Get the output filename.  It is the name of the file that the client should receive.
     *
     * @param layout the layout that will be printed (it can affect the filename chosen).
     * @param defaultName the default name (from configuration)
     */
    public final String getOutputFilename(final String layout, final String defaultName) {
        final String name = this.configuration.getOutputFilename(layout);
        return name == null ? defaultName : name;
    }
}
