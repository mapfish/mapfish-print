package org.mapfish.print.processor.http;

import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This processor maps uris submitted to the {@link org.mapfish.print.http.MfClientHttpRequestFactory} to
 * a modified uri as specified by the mapping parameter.</p>
 * <p>Example: change the hostname of all requests that are http requests and have the hostname: myhost.com
 * to localhost instead of myhost.com</p>
 * <pre><code>
 * - !mapUri
 *   mapping: {(http)://myhost.com(.*) : "$1://localhost$2"}
 * </code></pre>
 *
 * <p>Can be applied conditionally using matchers, like in {@link RestrictUrisProcessor}
 * (<a href="processors.html#!restrictUris">!restrictUris</a>
 * ).</p> [[examples=http_processors]]
 */
public final class MapUriProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapUriProcessor.class);
    private final Map<Pattern, String> uriMapping = new HashMap<>();

    /**
     * Set the uri mappings.
     * <p>
     * The key is a regular expression that must match uri's string form. The value will be used for the
     * replacement.
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
    public MfClientHttpRequestFactory createFactoryWrapper(
            final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
            final MfClientHttpRequestFactory requestFactory) {
        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory, matchers, false) {
            @Override
            protected ClientHttpRequest createRequest(
                    final URI uri,
                    final HttpMethod httpMethod,
                    final MfClientHttpRequestFactory requestFactory) throws IOException {
                final String uriString = uri.toString();
                for (Map.Entry<Pattern, String> entry: MapUriProcessor.this.uriMapping.entrySet()) {
                    Matcher matcher = entry.getKey().matcher(uriString);
                    if (matcher.matches()) {
                        LOGGER.debug("URI {} matched {}", uriString, entry.getKey());
                        final String finalUri = matcher.replaceAll(entry.getValue());
                        try {
                            return requestFactory.createRequest(new URI(finalUri), httpMethod);
                        } catch (URISyntaxException e) {
                            throw ExceptionUtils.getRuntimeException(e);
                        }
                    } else {
                        LOGGER.debug("URI {} did not match {}", uriString, entry.getKey());
                    }
                }
                return requestFactory.createRequest(uri, httpMethod);
            }
        };
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        super.extraValidation(validationErrors, configuration);
        if (this.uriMapping.isEmpty()) {
            validationErrors.add(new IllegalArgumentException("No uri mappings were defined"));
        }
    }
}
