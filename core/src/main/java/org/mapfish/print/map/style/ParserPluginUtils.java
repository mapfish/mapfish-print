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

package org.mapfish.print.map.style;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import org.geotools.styling.Style;
import org.mapfish.print.http.MapfishClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utilities for creating parser plugins.
 *
 * @author Jesse on 7/30/2014.
 */
public final class ParserPluginUtils {

    private ParserPluginUtils() {
        // utility class
    }

    /**
     * Load data using {@link org.mapfish.print.config.Configuration#loadFile(String)} and using http.  If data is able to be loaded
     * it will be passed to the loadFunction to be turned into a style.
     *
     * @param clientHttpRequestFactory the factory to use for http requests
     * @param styleRef                 the uri/file/else for attempting to load a style
     * @param loadFunction             the function to call when data has been loaded.
     */
    public static Optional<Style> loadStyleAsURI(final MapfishClientHttpRequestFactory clientHttpRequestFactory, final String styleRef,
                                                 final Function<byte[], Optional<Style>> loadFunction) throws IOException {
        HttpStatus statusCode;
        final byte[] input;

        Closer closer = Closer.create();
        try {
            URI uri;
            try {
                uri = new URI(styleRef);
            } catch (URISyntaxException e) {
                uri = new File(styleRef).toURI();
            }

            final ClientHttpRequest request = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            final ClientHttpResponse response = closer.register(request.execute());
            statusCode = response.getStatusCode();
            input = ByteStreams.toByteArray(response.getBody());
        } catch (Exception e) {
            return Optional.absent();
        } finally {
            closer.close();
        }
        if (statusCode == HttpStatus.OK) {
            return loadFunction.apply(input);
        } else {
            return Optional.absent();
        }
    }
}
