package org.mapfish.print.processor.http;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.http.matcher.UriMatchers;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>This processor allows adding static headers to an http request.</p>
 * <p>Example: add a Cookie header with multiple header values and add header2 with only one value</p>
 * <pre><code>
 * - !addHeaders
 *   headers:
 *     Cookie : [cookie-value, cookie-value2]
 *     Header2 : header2-value
 * </code></pre>
 *
 * <p>Can be applied conditionally using matchers, like in {@link RestrictUrisProcessor}
 * (<a href="processors.html#!restrictUris">!restrictUris</a>
 * ).</p> [[examples=http_processors]]
 */
public final class AddHeadersProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private final Map<String, List<String>> headers = new HashMap<>();

    /**
     * Create a MfClientHttpRequestFactory for adding the specified headers.
     *
     * @param requestFactory the basic request factory.  It should be unmodified and just wrapped with
     *         a proxy class.
     * @param matchers The matchers.
     * @param headers The headers.
     */
    public static MfClientHttpRequestFactory createFactoryWrapper(
            final MfClientHttpRequestFactory requestFactory,
            final UriMatchers matchers, final Map<String, List<String>> headers) {
        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory, matchers, false) {
            @Override
            protected ClientHttpRequest createRequest(
                    final URI uri,
                    final HttpMethod httpMethod,
                    final MfClientHttpRequestFactory requestFactory) throws IOException {
                final ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
                request.getHeaders().putAll(headers);
                return request;
            }
        };
    }

    /**
     * A map of the header key value pairs.  Keys are strings and values are either list of strings or a
     * string.
     *
     * @param headers the header map
     */
    @SuppressWarnings("unchecked")
    public void setHeaders(final Map<String, Object> headers) {
        this.headers.clear();
        for (Map.Entry<String, Object> entry: headers.entrySet()) {
            if (entry.getValue() instanceof List) {
                List value = (List) entry.getValue();
                // verify they are all strings
                for (Object o: value) {
                    Assert.isTrue(o instanceof String, o + " is not a string it is a: '" +
                            o.getClass() + "'");
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
    protected void extraValidation(
            final List<Throwable> validationErrors,
            final Configuration configuration) {
        super.extraValidation(validationErrors, configuration);
        if (this.headers.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no headers defined."));
        }
    }

    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(
            final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
            final MfClientHttpRequestFactory requestFactory) {
        return createFactoryWrapper(requestFactory, this.matchers, this.headers);
    }
}
