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

import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.http.matcher.AcceptAllMatcher;
import org.mapfish.print.processor.http.matcher.HostMatcher;
import org.mapfish.print.processor.http.matcher.MatchInfo;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * This processor check urls against a set of url matchers to see if the request should be allowed or rejected.
 * <p>
 *     Usage of processor is as follows:
 * </p>
 * <pre><code>
 * - !restrictUris
 *   matchers:
 *     - !localMatch {}
 *     - !ipMatch
 *       ip: www.camptocamp.org
 *     - !dnsMatch
 *       host: mapfish-geoportal.demo-camptocamp.com
 *       port: 80
 *     - !dnsMatch
 *       host: labs.metacarta.com
 *       port: 80
 *     - !dnsMatch
 *       host: terraservice.net
 *       port: 80
 *     - !dnsMatch
 *       host: tile.openstreetmap.org
 *       port: 80
 *     - !dnsMatch
 *       host: www.geocat.ch
 *       port: 80
 * </code></pre>
 *
 * <p>
 *     <strong>Note:</strong> if this class is part of a CompositeClientHttpRequestFactoryProcessor (!configureHttpRequests) then
 *     it should be the last one so that the checks are done after all changes to the URIs
 * </p>
 * @see org.mapfish.print.processor.http.matcher.AcceptAllMatcher
 * @see org.mapfish.print.processor.http.matcher.AddressHostMatcher
 * @see org.mapfish.print.processor.http.matcher.DnsHostMatcher
 * @see org.mapfish.print.processor.http.matcher.LocalHostMatcher
 *
 * @author Jesse on 8/6/2014.
 */
public final class RestrictUrisProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private List<? extends URIMatcher> matchers = Collections.singletonList(new AcceptAllMatcher());

    /**
     * The matchers used to select the legal urls. For example:
     * <pre><code>
     * - !restrictUris
     *   matchers:
     *     - !localMatch
     *       dummy: true
     *     - !ipMatch
     *     ip: www.camptocamp.org
     *     - !dnsMatch
     *       host: mapfish-geoportal.demo-camptocamp.com
     *       port: 80
     *     - !dnsMatch
     *       host: labs.metacarta.com
     *       port: 80
     *     - !dnsMatch
     *       host: terraservice.net
     *       port: 80
     *     - !dnsMatch
     *       host: tile.openstreetmap.org
     *       port: 80
     *     - !dnsMatch
     *       host: www.geocat.ch
     *       port: 80
     * </code></pre>
     *
     * @param matchers the list of matcher to use to check if a url is permitted
     */
    public void setMatchers(final List<? extends HostMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.matchers == null) {
            validationErrors.add(new IllegalArgumentException("Matchers cannot be null.  There should be at least a !acceptAll matcher"));
        }
        if (this.matchers != null && this.matchers.isEmpty()) {
            validationErrors.add(new IllegalArgumentException("There are no url matchers defined.  There should be at least a " +
                                                              "!acceptAll matcher"));
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
                for (URIMatcher matcher : RestrictUrisProcessor.this.matchers) {
                    if (matcher.accepts(MatchInfo.fromUri(uri, httpMethod))) {
                        return requestFactory.createRequest(uri, httpMethod);
                    }
                }
                throw new IllegalArgumentException(uri + " is not one of the permitted urls.");
            }
        };
    }
}
