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

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.processor.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a report template configuration.
 *
 * @author sbrunner
 */
public class Template implements ConfigurationObject {
    private String jasperTemplate;
    private Map<String, Attribute<?>> attributes;
    private List<Processor> processors;
    private String iterValue;
    private List<Processor> iterProcessors = new ArrayList<Processor>();

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

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
            this.attributes.get(name).printClientConfig(json);
            json.endObject();
        }
        json.endArray();
    }

    public final Map<String, Attribute<?>> getAttributes() {
        return this.attributes;
    }

    public final void setAttributes(final Map<String, Attribute<?>> attributes) {
        this.attributes = attributes;
    }

    public final String getJasperTemplate() {
        return this.jasperTemplate;
    }

    public final void setJasperTemplate(final String jasperTemplate) {
        this.jasperTemplate = jasperTemplate;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    public final void setProcessors(final List<Processor> processors) {
        this.processors = processors;
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

    public final void setIterProcessors(final List<Processor> iterProcessors) {
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
}
