/*
 * Copyright (C) 2014-2015  Camptocamp
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.http.matcher.URIMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * This processor forwards all the headers from the print request (from the Mapfish Print client) to each http request made for
 * the particular print job.  All headers can be forwarded (if forwardAll is set to true) or the specific headers to forward
 * can be specified.
 * <p>Example 1: Forward all headers from print request</p>
 * <pre><code>
 * - !forwardHeaders
 *   all: true
 * </code></pre>
 * <p>Example 2: Forward specific headers (header1 and header2 will be forwarded)</p>
 * <pre><code>
 * - !forwardHeaders
 *   headers: [header1, header2]
 * </code></pre>
 *
 *  Can be applied conditionally using matchers, like in {@link RestrictUrisProcessor} (!restrictUris).
 *
 * @author Jesse on 6/26/2014.
 */
public final class ForwardHeadersProcessor
        extends AbstractProcessor<ForwardHeadersProcessor.Param, ClientHttpFactoryProcessorParam>
        implements HttpProcessor<ForwardHeadersProcessor.Param> {

    private final AddHeadersProcessor addHeadersProcessor = new AddHeadersProcessor();

    private Set<String> headerNames = Sets.newHashSet();
    private boolean forwardAll = false;

    /**
     * Constructor.
     */
    public ForwardHeadersProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    /**
     * Set the header names to forward from the request.  Should not be defined if all is set to true
     *
     * @param names the header names.
     */
    public void setHeaders(final Set<String> names) {
        // transform to lower-case because header names should be case-insensitive
        Set<String> lowerCaseNames = new HashSet<String>();
        for (String name : names) {
            lowerCaseNames.add(name.toLowerCase());
        }
        this.headerNames = lowerCaseNames;
    }

    /**
     * The matchers used to select the urls that are going to be modified by the processor.
     * For example:
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
    public void setMatchers(final List<? extends URIMatcher> matchers) {
        this.addHeadersProcessor.setMatchers(matchers);
    }

    /**
     * If set to true then all headers are forwarded.  If this is true headers should be empty (or undefined)
     *
     * @param all if true forward all headers
     */
    public void setAll(final boolean all) {
        this.forwardAll = all;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (!this.forwardAll && this.headerNames.isEmpty()) {
            validationErrors.add(new IllegalStateException("all is false and no headers are defined"));
        }
        if (this.forwardAll && !this.headerNames.isEmpty()) {
            validationErrors.add(new IllegalStateException("all is true but headers is defined. Either all is true " +
                                                           "OR headers is specified"));
        }
    }

    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(final Param param,
                                                         final MfClientHttpRequestFactory requestFactory) {
        Map<String, Object> headers = Maps.newHashMap();

        for (Map.Entry<String, List<String>> entry : param.requestHeaders.getHeaders().entrySet()) {
            if (ForwardHeadersProcessor.this.forwardAll ||
                    ForwardHeadersProcessor.this.headerNames.contains(entry.getKey().toLowerCase())) {
                headers.put(entry.getKey(), entry.getValue());
            }
        }
        this.addHeadersProcessor.setHeaders(headers);

        return this.addHeadersProcessor.createFactoryWrapper(param, requestFactory);
    }

    @Nullable
    @Override
    public Param createInputParameter() {
        return new Param();
    }

    @Nullable
    @Override
    public ClientHttpFactoryProcessorParam execute(final Param values, final ExecutionContext context) throws Exception {
        values.clientHttpRequestFactory = createFactoryWrapper(values, values.clientHttpRequestFactory);
        return values;
    }

    /**
     * The parameters required by this processor.
     */
    public static class Param extends ClientHttpFactoryProcessorParam {
        /**
         * The http headers from the print request.
         */
        public HttpRequestHeadersAttribute.Value requestHeaders;
    }

}
