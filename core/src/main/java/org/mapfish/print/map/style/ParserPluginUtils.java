package org.mapfish.print.map.style;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.styling.Style;
import org.mapfish.print.config.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utilities for creating parser plugins.
 */
public final class ParserPluginUtils {

    private ParserPluginUtils() {
        // utility class
    }

    /**
     * Load data using {@link Configuration#loadFile(String)} and using http.  If
     * data is able to be loaded it will be passed to the loadFunction to be turned into a style.
     *
     * @param clientHttpRequestFactory the factory to use for http requests
     * @param styleRef the uri/file/else for attempting to load a style
     * @param loadFunction the function to call when data has been loaded.
     */
    public static Optional<Style> loadStyleAsURI(
            final ClientHttpRequestFactory clientHttpRequestFactory, final String styleRef,
            final Function<byte[], @Nullable Optional<Style>> loadFunction) {
        HttpStatus statusCode;
        final byte[] input;

        URI uri;
        try {
            uri = new URI(styleRef);
        } catch (URISyntaxException e) {
            uri = new File(styleRef).toURI();
        }

        try {
            final ClientHttpRequest request = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            try (ClientHttpResponse response = request.execute()) {
                statusCode = response.getStatusCode();
                input = IOUtils.toByteArray(response.getBody());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        if (statusCode == HttpStatus.OK) {
            return loadFunction.apply(input);
        } else {
            return Optional.empty();
        }
    }
}
