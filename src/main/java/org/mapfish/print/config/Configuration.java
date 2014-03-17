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

import java.util.*;

/**
 * The Main Configuration Bean.
 * <p/>
 * Created by Jesse on 2/20/14.
 */
public class Configuration {
    private boolean reloadConfig;
    private String proxyBaseUrl;
    private TreeSet<String> headers;
    private List<HostMatcher> hosts = new ArrayList<HostMatcher>();
    private List<SecurityStrategy> security = Collections.emptyList();
    private Map<String, Template> templates;

    /**
     * Print out the configuration that the client needs to make a request.
     *
     * @param json the output writer.
     *
     * @throws JSONException
     */
    public void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("layouts");
        json.array();
        for (String name : this.templates.keySet()) {
            json.object();
            json.key("name").value(name);
            this.templates.get(name).printClientConfig(json);
            json.endObject();
        }
        json.endArray();
    }

    public boolean isReloadConfig() {
        return this.reloadConfig;
    }

    public void setReloadConfig(final boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }

    public String getProxyBaseUrl() {
        return this.proxyBaseUrl;
    }

    public void setProxyBaseUrl(final String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
    }

    public TreeSet<String> getHeaders() {
        return headers;
    }

    public void setHeaders(final TreeSet<String> headers) {
        this.headers = headers;
    }

    /**
     * Calculate the name of the pdf file to return to the user.
     *
     * @param layoutName the name of file from the configuration.
     */
    public String getOutputFilename(final String layoutName) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public List<HostMatcher> getHosts() {
        return this.hosts;
    }

    public void setHosts(final List<HostMatcher> hosts) {
        this.hosts = hosts;
    }

    public List<SecurityStrategy> getSecurity() {
        return this.security;
    }

    public void setSecurity(final List<SecurityStrategy> security) {
        this.security = security;
    }

    public Map<String, Template> getTemplates() {
        return this.templates;
    }

    /**
     * Retrieve the configuration of the named template.
     * @param name the template name;
     */
    public Template getTemplate(final String name) {
        return this.templates.get(name);
    }

    public void setTemplates(final Map<String, Template> templates) {
        this.templates = templates;
    }
}
