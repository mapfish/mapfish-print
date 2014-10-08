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

package org.mapfish.print.processor.http;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.RegexpUtil;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This processor maps https requests to http requests for certain hosts. The port number can also be mapped since that is usually
 * required.
 * <p>Example: </p>
 * <pre><code>
 * - !useHttpForHttps
 *   hosts: [localhost, www.camptocamp.com]
 *   portMapping:
 *     443 : 80
 *     8443 : 8443
 * </code></pre>
 *
 * @author Jesse on 6/25/2014.
 */
public final class UseHttpForHttpsProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private static final int HTTPS_STANDARD_PORT = 443;
    private static final int HTTP_STANDARD_PORT = 80;
    private static final int JAVA_HTTPS_STANDARD_PORT = 8443;
    private static final int JAVA_HTTP_STANDARD_PORT = 8080;
    private static final Pattern HTTP_AUTHORITY_PORT_EXTRACTOR = Pattern.compile("(.*@)?.*:(\\d+)");
    private static final Pattern HTTP_AUTHORITY_HOST_EXTRACTOR = Pattern.compile("(.*@)?([^:]*)(:\\d+)?");
    private Map<Integer, Integer> portMapping = Maps.newHashMap();
    private List<Pattern> hosts = Lists.newArrayList();

    /**
     * Constructor.
     */
    protected UseHttpForHttpsProcessor() {
        this.portMapping.put(HTTPS_STANDARD_PORT, HTTP_STANDARD_PORT);
        this.portMapping.put(JAVA_HTTPS_STANDARD_PORT, JAVA_HTTP_STANDARD_PORT);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.hosts.isEmpty()) {
            validationErrors.add(new IllegalArgumentException("No hosts are registered"));
        }
    }

    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
                                                         final MfClientHttpRequestFactory requestFactory) {
        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory) {
            @Override
            protected ClientHttpRequest createRequest(final URI uri,
                                                      final HttpMethod httpMethod,
                                                      final MfClientHttpRequestFactory requestFactory) throws IOException {
                if (uri.getScheme().equals("https")) {
                    try {
                        URI httpUri = uri;
                        if (uri.getHost() == null && uri.getAuthority() != null) {
                            final Matcher matcher = HTTP_AUTHORITY_HOST_EXTRACTOR.matcher(uri.getAuthority());
                            if (matcher.matches()) {
                                final String host = matcher.group(2);
                                if (matchingHost(host)) {
                                    httpUri = updatePortAndSchemeInAuthority(uri);
                                }
                            }
                        } else {
                            if (matchingHost(uri.getHost())) {
                                httpUri = updatePortAndScheme(uri);
                            }
                        }
                        return requestFactory.createRequest(httpUri, httpMethod);
                    } catch (URISyntaxException e) {
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                }
                return requestFactory.createRequest(uri, httpMethod);
            }
        };
    }

    /**
     * Set the https port to http port mapping.
     *
     * @param portMapping the mappings to add.
     */
    public void setPortMapping(final Map<Integer, Integer> portMapping) {
        this.portMapping.putAll(portMapping);
    }

    /**
     * Set the patterns to use for selecting the hosts to apply the https -> http mapping to.
     * <ul>
     * <li>If the host starts and ends with / then it is compiled as a regular expression</li>
     * <li>Otherwise the hosts must exactly match</li>
     * </ul>
     *
     * @param hosts hosts to match.  Can be regular expressions
     */
    public void setHosts(final List<String> hosts) {
        this.hosts.clear();
        for (String host : hosts) {
            this.hosts.add(RegexpUtil.compilePattern(host));
        }
    }

    private boolean matchingHost(final String host) {
        for (Pattern hostPattern : UseHttpForHttpsProcessor.this.hosts) {
            if (hostPattern.matcher(host).matches()) {
                return true;
            }
        }
        return false;
    }

    private URI updatePortAndScheme(final URI uri) throws URISyntaxException {
        URI httpUri;
        int port = uri.getPort();
        if (UseHttpForHttpsProcessor.this.portMapping.containsKey(port)) {
            port = UseHttpForHttpsProcessor.this.portMapping.get(port);
        }

        httpUri = new URI("http", uri.getUserInfo(), uri.getHost(), port,
                uri.getPath(),
                uri.getQuery(), uri.getFragment());
        return httpUri;
    }

    private URI updatePortAndSchemeInAuthority(final URI uri) throws URISyntaxException {
        URI httpUri;
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
        return httpUri;
    }
}
