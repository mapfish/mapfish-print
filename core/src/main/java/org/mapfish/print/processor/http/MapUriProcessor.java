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
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This processor maps uris submitted to the {@link org.springframework.http.client.ClientHttpRequestFactory} to a modified uri
 * as specified by the mapping parameter.
 *
 * @author Jesse on 6/25/2014.
 */
public final class MapUriProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private final Map<Pattern, String> uriMapping = Maps.newHashMap();

    /**
     * Set the uri mappings.
     *
     * The key is a regular expression that must match uri's string form. The value will be used for the replacement.
     *
     * @param mapping the uri mappings.
     */
    public void setMapping(final Map<String, String> mapping) {
        this.uriMapping.clear();
        for (Map.Entry<String, String> entry: mapping.entrySet()) {
           Pattern pattern = Pattern.compile(entry.getKey());
            this.uriMapping.put(pattern, entry.getValue());
        }
    }

    @Override
    public ClientHttpRequestFactory createFactoryWrapper(final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
                                                         final ClientHttpRequestFactory requestFactory) {
        return new AbstractClientHttpRequestFactoryWrapper(requestFactory) {
            @Override
            protected ClientHttpRequest createRequest(final URI uri,
                                                      final HttpMethod httpMethod,
                                                      final ClientHttpRequestFactory requestFactory) throws IOException {
                final String uriString = uri.toString();
                for (Map.Entry<Pattern, String> entry : MapUriProcessor.this.uriMapping.entrySet()) {
                    Matcher matcher = entry.getKey().matcher(uriString);
                    if (matcher.matches()) {
                        final String finalUri = matcher.replaceAll(entry.getValue());
                        try {
                            return requestFactory.createRequest(new URI(finalUri), httpMethod);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return requestFactory.createRequest(uri, httpMethod);
            }
        };
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.uriMapping.isEmpty()) {
            validationErrors.add(new IllegalArgumentException("No uri mappings were defined"));
        }
    }
}
