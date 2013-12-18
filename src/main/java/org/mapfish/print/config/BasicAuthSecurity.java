package org.mapfish.print.config;

import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

public class BasicAuthSecurity extends SecurityStrategy {

    String username = null;
    String password = null;
    boolean preemptive = false;

    @Override
    public void configure(URI uri, HttpClient httpClient) {
        if(username==null || password==null) throw new IllegalStateException("username and password configuration of BasicAuthSecurity is required");

        if(preemptive) {
            httpClient.getParams().setAuthenticationPreemptive(true);
        }
        httpClient.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, password));
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }
}
