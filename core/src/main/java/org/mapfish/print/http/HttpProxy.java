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

package org.mapfish.print.http;

import org.apache.http.HttpHost;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.http.matcher.URIMatcher;

import java.util.List;

/**
 * This configuration object configures the proxies to be used by the system.  This is configured as
 * one of the root elements of the config.yaml
 *
 * <p>Example - Explicitly declare that requests to localhost or www.camptocamp.org require proxying: </p>
 * <pre><code>
 * proxies:
 *   - !proxy
 *     requireProxy:
 *       - !localMatch {}
 *       - !ipMatch
 *         ip: www.camptocamp.org
 *     host: proxy.host.com
 *     port: 8888
 *     username: username
 *     password: xyzpassword
 * </code></pre>
 * <p>Example - Proxy all requests except localhost and www.camptocamp.org: </p>
 * <pre><code>
 * proxies:
 *   - !proxy
 *     noProxy:
 *       - !localMatch {}
 *       - !ipMatch
 *         ip: www.camptocamp.org
 *     scheme: http
 *     host: proxy.host.com
 *     port: 8888
 *     username: username
 *     password: xyzpassword
 * </code></pre>
 *
 * @author Jesse on 6/25/2014.
 */
public final class HttpProxy extends HttpCredential {
    private String host;
    // CSOFF: MagicNumber
    private int port = 80;
    // CSON: MagicNumber
    private String scheme;

    public HttpHost getHttpHost() {
        return new HttpHost(this.host, this.port, getScheme());
    }

    private String getScheme() {
        if (this.scheme == null) {
            if (super.getUsername() == null) {
                return "http";
            } else {
                return "https";
            }
        }
        return this.scheme;
    }

    /**
     * Matchers are used to choose which requests this proxy applies to.
     *
     * @see org.mapfish.print.processor.http.matcher.URIMatcher
     * @see org.mapfish.print.processor.http.RestrictUrisProcessor
     *
     * @param matchers the matchers to use to determine which requests the applies can be used for
     */
    public void setMatchers(final List<? extends URIMatcher> matchers) {
        super.setMatchers(matchers);
    }

    /**
     * The host of the proxy.  Can be a hostname or ip address.
     * <p>
     * This is required.
     * </p>
     *
     * @param host the host of the proxy
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * The scheme (http, https) of the proxy.
     * <p>
     * This is optional, default is http if no username and https if there is a password
     * </p>
     *
     * @param scheme the scheme of the proxy
     */
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    /**
     * The username for authenticating with the proxy.
     * <p>
     * This is optional
     * </p>
     *
     * @param username the username for authenticating with the proxy
     */
    public void setUsername(final String username) {
        super.setUsername(username);
    }

    /**
     * The password for authenticating with the proxy.
     * <p>
     * This is optional
     * </p>
     *
     * @param password the password for authenticating with the proxy
     */
    public  void setPassword(final String password) {
        super.setPassword(password);
    }

    /**
     * The host of the proxy.  Can be a hostname or ip address.
     * <p>
     * This is optional.  The default value is 80.
     * </p>
     *
     * @param port the port of the proxy
     */
    public  void setPort(final int port) {
        this.port = port;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.host == null) {
            validationErrors.add(new IllegalStateException("The parameter 'host' is required."));
        }
    }

}
