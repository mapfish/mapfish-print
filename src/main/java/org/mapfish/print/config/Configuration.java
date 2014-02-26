package org.mapfish.print.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONWriter;

/**
 * The Main Configuration Bean.
 *
 * Created by Jesse on 2/20/14.
 */
public class Configuration {
    private boolean reloadConfig;
    private String proxyBaseUrl;
    private TreeSet<String> headers;
    private List<HostMatcher> hosts = new ArrayList<HostMatcher>();
    private List<SecurityStrategy> security = Collections.emptyList();
    private Map<String, Template> templates;

    public void printClientConfig(JSONWriter json) throws JSONException {
        json.key("layouts");
        json.array();
        for (String name : templates.keySet()) {
            json.object();
            json.key("name").value(name);
            templates.get(name).printClientConfig(json);
            json.endObject();
        }
        json.endArray();
    }

    public boolean isReloadConfig() {
        return reloadConfig;
    }

    public void setReloadConfig(boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }

    public String getProxyBaseUrl() {
        return proxyBaseUrl;
    }

    public void setProxyBaseUrl(String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
    }

    public TreeSet<String> getHeaders() {
        return headers;
    }

    public void setHeaders(TreeSet<String> headers) {
        this.headers = headers;
    }

    public String getOutputFilename(String layoutName) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public List<HostMatcher> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostMatcher> hosts) {
        this.hosts = hosts;
    }

    public List<SecurityStrategy> getSecurity() {
        return security;
    }

    public void setSecurity(List<SecurityStrategy> security) {
        this.security = security;
    }

    public Map<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, Template> templates) {
        this.templates = templates;
    }
}
