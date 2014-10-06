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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.http.matcher.MatchInfo;
import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * A Route planner that obtains proxies from the configuration that is currently in
 * {@link org.mapfish.print.http.MfClientHttpRequestFactoryImpl#CURRENT_CONFIGURATION}.
 *
 * {@link MfClientHttpRequestFactoryImpl.Request} will set the correct configuration
 * before the request is executed so that correct proxies will be set.
 *
 * @author Jesse on 9/4/2014.
 */
public final class MfRoutePlanner extends DefaultRoutePlanner {
    /**
     * Constructor.
     */
    public MfRoutePlanner() {
        super(null);
    }

    @Override
    protected HttpHost determineProxy(final HttpHost target,
                                      final HttpRequest request,
                                      final HttpContext context) throws HttpException {
        Configuration config = MfClientHttpRequestFactoryImpl.getCurrentConfiguration();
        if (config == null) {
            return null;
        }
        final URI uri;
        try {
            uri = new URI(request.getRequestLine().getUri());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpMethod method = HttpMethod.valueOf(request.getRequestLine().getMethod());

        final List<HttpProxy> proxies = config.getProxies();
        for (HttpProxy proxy : proxies) {
            try {
                if (proxy.matches(MatchInfo.fromUri(uri, method))) {
                    return proxy.getHttpHost();
                }
            } catch (SocketException e) {
                throw new HttpException(e.getMessage(), e);
            } catch (UnknownHostException e) {
                throw new HttpException(e.getMessage(), e);
            } catch (MalformedURLException e) {
                throw new HttpException(e.getMessage(), e);
            }
        }
        return null;
    }
}
