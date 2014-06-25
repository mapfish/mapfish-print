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

package org.mapfish.print.processor;

import com.google.common.collect.Maps;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * @author Jesse on 6/25/2014.
 */
public final class UseHttpForHttpsProcessor extends AbstractProcessor<UseHttpForHttpsProcessor.Param, UseHttpForHttpsProcessor.Param> {
    static final int HTTPS_STANDARD_PORT = 443;
    static final int HTTP_STANDARD_PORT = 80;
    static final int JAVA_HTTPS_STANDARD_PORT = 8443;
    static final int JAVA_HTTP_STANDARD_PORT = 8080;
    private static final Pattern HTTP_AUTHORITY_PORT_EXTRACTOR = Pattern.compile("(.*@)?.*:(\\d+)");
    private Map<Integer, Integer> portMapping = Maps.newHashMap();

    /**
     * Constructor.
     */
    protected UseHttpForHttpsProcessor() {
        super(Param.class);
        this.portMapping.put(HTTPS_STANDARD_PORT, HTTP_STANDARD_PORT);
        this.portMapping.put(JAVA_HTTPS_STANDARD_PORT, JAVA_HTTP_STANDARD_PORT);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        // nothing to do
    }

    @Nullable
    @Override
    public Param createInputParameter() {
        return new Param();
    }

    @Nullable
    @Override
    public Param execute(final Param values, final ExecutionContext context) throws Exception {
        values.clientHttpRequestFactory = new UseHttpForHttpsHttpRequestFactory(values.clientHttpRequestFactory);
        return values;
    }

    /**
     * Update the port mapping with the new mappings.
     *
     * @param portMapping the mappings to add.
     */
    public void setPortMapping(final Map<Integer, Integer> portMapping) {
        this.portMapping.putAll(portMapping);
    }

    static class Param {
        /**
         * The object for creating requests.  There should always be an instance in the values object
         * so it does not need to be created.
         */
        public ClientHttpRequestFactory clientHttpRequestFactory;
    }

    private final class UseHttpForHttpsHttpRequestFactory implements ClientHttpRequestFactory {
        private final ClientHttpRequestFactory wrappedFactory;

        private UseHttpForHttpsHttpRequestFactory(final ClientHttpRequestFactory wrappedFactory) {
            this.wrappedFactory = wrappedFactory;
        }

        @Override
        public ClientHttpRequest createRequest(final URI uri, final HttpMethod httpMethod) throws IOException {
            if (uri.getScheme().equals("https")) {
                try {
                    URI httpUri;
                    if (uri.getHost() == null && uri.getAuthority() != null) {
                        String authority = uri.getAuthority();

                        Matcher matcher = HTTP_AUTHORITY_PORT_EXTRACTOR.matcher(uri.getAuthority());
                        if (matcher.matches()) {
                            int port = Integer.parseInt(matcher.group(2));
                            authority = authority.substring(0, matcher.start(2));

                            if (UseHttpForHttpsProcessor.this.portMapping.containsKey(port)) {
                                port = UseHttpForHttpsProcessor.this.portMapping.get(port);
                            }

                            authority = authority + port;
                        }

                        httpUri = new URI("http", authority, uri.getPath(), uri.getQuery(),
                                uri.getFragment());
                    } else {
                        int port = uri.getPort();
                        if (UseHttpForHttpsProcessor.this.portMapping.containsKey(port)) {
                            port = UseHttpForHttpsProcessor.this.portMapping.get(port);
                        }
                        httpUri = new URI("http", uri.getUserInfo(), uri.getHost(), port,
                                uri.getPath(),
                                uri.getPath(), uri.getFragment());
                    }
                    return this.wrappedFactory.createRequest(httpUri, httpMethod);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.wrappedFactory.createRequest(uri, httpMethod);
        }
    }
}
