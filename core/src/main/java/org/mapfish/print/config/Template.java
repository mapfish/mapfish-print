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

import com.google.common.base.Optional;

import org.geotools.styling.Style;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Represents a report template configuration.
 *
 * @author sbrunner
 */
public class Template implements ConfigurationObject, HasConfiguration {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Template.class);
    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;


    private String jasperTemplate;
    private Map<String, Attribute> attributes;
    private List<Processor> processors;
    private String iterValue;
    private List<Processor> iterProcessors = new ArrayList<Processor>();

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private volatile ProcessorDependencyGraph processorGraph;
    private volatile ProcessorDependencyGraph iterProcessorGraph;
    private Map<String, Style> styles = new HashMap<String, Style>();
    private Configuration configuration;
    @Autowired
    private StyleParser styleParser;

    /**
     * Print out the template information that the client needs for performing a request.
     *
     * @param json the writer to write the information to.
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("attributes");
        json.array();
        for (String name : this.attributes.keySet()) {
            json.object();
            json.key("name").value(name);
            this.attributes.get(name).printClientConfig(json, this);
            json.endObject();
        }
        json.endArray();
    }

    public final Map<String, Attribute> getAttributes() {
        return this.attributes;
    }

    /**
     * Set the attributes for this template.
     *
     * @param attributes the attribute map
     */
    public final void setAttributes(final Map<String, Attribute> attributes) {
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            Object attribute = entry.getValue();
            if (!(attribute instanceof Attribute)) {
                final String msg = "Attribute: '" + entry.getKey() + "' is not an attribute. It is a: " + attribute;
                LOGGER.error("Error setting the Attributes: " + msg);
                throw new IllegalArgumentException(msg);
            }
        }
        this.attributes = attributes;
    }

    public final String getJasperTemplate() {
        return this.jasperTemplate;
    }

    public final void setJasperTemplate(final String jasperTemplate) {
        this.jasperTemplate = jasperTemplate;
    }

    /**
     * Set the normal processors.
     *
     * @param processors the processors to set.
     */
    public final void setProcessors(final List<Processor> processors) {
        assertProcessors(processors);
        this.processors = processors;
    }

    private void assertProcessors(final List<Processor> processorsToCheck) {
        for (Processor entry : processorsToCheck) {
            if (!(entry instanceof Processor)) {
                final String msg = "Processor: " + entry + " is not a processor.";
                LOGGER.error("Error setting the Attributes: " + msg);
                throw new IllegalArgumentException(msg);
            }
        }

    }

    public final String getIterValue() {
        return this.iterValue;
    }

    public final void setIterValue(final String iterValue) {
        this.iterValue = iterValue;
    }

    public final List<Processor> getIterProcessors() {
        return this.iterProcessors;
    }

    /**
     * Set the processors that require Iterable inputs.
     *
     * @param iterProcessors the processors to set.
     */
    public final void setIterProcessors(final List<Processor> iterProcessors) {
        assertProcessors(iterProcessors);
        this.iterProcessors = iterProcessors;
    }

    public final String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public final void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public final String getJdbcUser() {
        return this.jdbcUser;
    }

    public final void setJdbcUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public final String getJdbcPassword() {
        return this.jdbcPassword;
    }

    public final void setJdbcPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * Get the processor graph to use for executing all the processors for the template.
     *
     * @return the processor graph.
     */
    public final ProcessorDependencyGraph getProcessorGraph() {
        if (this.processorGraph == null) {
            synchronized (this) {
                if (this.processorGraph == null) {
                    this.processorGraph = this.processorGraphFactory.build(this.processors);
                }
            }
        }
        return this.processorGraph;
    }

    /**
     * Get the processor graph to use for executing all the iter processors for the template.
     *
     * @return the processor graph.
     */
    public final ProcessorDependencyGraph getIterProcessorGraph() {
        if (this.iterProcessorGraph == null) {
            synchronized (this) {
                if (this.iterProcessorGraph == null) {
                    this.iterProcessorGraph = this.processorGraphFactory.build(this.iterProcessors);
                }
            }
        }
        return this.iterProcessorGraph;
    }

    /**
     * Set the named styles defined in the configuration for this.
     *
     * @param styles set the styles specific for this template.
     */
    public final void setStyles(final Map<String, String> styles) {
        Map<String, Style> map = StyleParser.loadStyles(this.configuration, this.styleParser, styles);

        this.styles = map;
    }

    /**
     * Look for a style in the named styles provided in the configuration.
     *
     * @param styleName the name of the style to look for.
     */
    @Nonnull
    public final Optional<Style> getStyle(final String styleName) {
        return Optional.fromNullable(this.styles.get(styleName))
                .or(this.configuration.getStyle(styleName));
    }

    @Override
    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public final Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Register the named style.
     * @param styleName the style name
     * @param style the style to register under that name.
     */
    public final void setStyle(final String styleName, final Style style) {
        this.styles.put(styleName, style);
    }
}
