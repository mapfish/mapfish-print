package org.mapfish.print.map.style;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import org.geotools.styling.Style;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utilities for creating parser plugins.
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
     * @param styleRef the uri/file/else for attempting to load a style
     * @param loadFunction the function to call when data has been loaded.
     */
    public static Optional<Style> loadStyleAsURI(
            final ClientHttpRequestFactory clientHttpRequestFactory, final String styleRef,
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
