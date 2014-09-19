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

import com.google.common.collect.Maps;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This processor allows adding static headers to an http request.
 * <p>Example: add a Cookie header with multiple header values and add header2 with only one value</p>
 * <pre><code>
 * - !addHeaders
 *   headers:
 *     Cookie : [cookie-value, cookie-value2]
 *     Header2 : header2-value
 * </code></pre>
 * @author Jesse on 6/26/2014.
 */
public final class AddHeadersProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private final Map<String, List<String>> headers = Maps.newHashMap();

    /**
     * A map of the header key value pairs.  Keys are strings and values are either list of strings or a string.
     * @param headers the header map
     */
    @SuppressWarnings("unchecked")
    public void setHeaders(final Map<String, Object> headers) {
        this.headers.clear();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getValue() instanceof List) {
                List value = (List) entry.getValue();
                // verify they are all strings
                for (Object o : value) {
                    Assert.isTrue(o instanceof String, o + " is not a string it is a: '" + o.getClass() + "'");
                }
                this.headers.put(entry.getKey(), (List<String>) entry.getValue());
            } else if (entry.getValue() instanceof String) {
                final List<String> value = Collections.singletonList((String) entry.getValue());
                this.headers.put(entry.getKey(), value);
            } else {
                throw new IllegalArgumentException("Only strings and list of strings may be headers");
            }
        }
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.headers.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no headers defined."));
        }
    }

    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
                                                         final MfClientHttpRequestFactory requestFactory) {
        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory) {
            @Override
            protected ClientHttpRequest createRequest(final URI uri,
                                                      final HttpMethod httpMethod,
                                                      final MfClientHttpRequestFactory requestFactory) throws
                    IOException {
                final ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
                request.getHeaders().putAll(AddHeadersProcessor.this.headers);
                return request;
            }
        };

    }
}
