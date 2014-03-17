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

import java.util.List;
import java.util.Map;

/**
 * Represents a report template configuration.
 */
public class Template implements ConfigurationObject {
    private String jasperTemplate;
    private Map<String, Attribute> attributes;
    private List<Processor> processors;
    private String iterValue;
    private List<Processor> iterProcessors;

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    /**
     * Print out the template information that the client needs for performing a request.
     *
     * @param json the writer to write the information to.
     */
    public void printClientConfig(final JSONWriter json) throws JSONException {
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

    public Map<String, Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Map<String, Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getJasperTemplate() {
        return this.jasperTemplate;
    }

    public void setJasperTemplate(final String jasperTemplate) {
        this.jasperTemplate = jasperTemplate;
    }

    public List<Processor> getProcessors() {
        return this.processors;
    }

    public void setProcessors(final List<Processor> processors) {
        this.processors = processors;
    }

    public String getIterValue() {
        return this.iterValue;
    }

    public void setIterValue(final String iterValue) {
        this.iterValue = iterValue;
    }

    public List<Processor> getIterProcessors() {
        return this.iterProcessors;
    }

    public void setIterProcessors(final List<Processor> iterProcessors) {
        this.iterProcessors = iterProcessors;
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUser() {
        return this.jdbcUser;
    }

    public void setJdbcUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public String getJdbcPassword() {
        return this.jdbcPassword;
    }

    public void setJdbcPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }
}
