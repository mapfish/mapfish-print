package org.mapfish.print.http;

import org.mapfish.print.processor.http.matcher.UriMatchers;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;

/**
 * The AbstractMfClientHttpRequestFactoryWrapper class.
 */
public abstract class AbstractMfClientHttpRequestFactoryWrapper implements MfClientHttpRequestFactory {

    private final MfClientHttpRequestFactory wrappedFactory;
    private final UriMatchers matchers;
    private final boolean failIfNotMatch;


    /**
     * Creates a {@code AbstractClientHttpRequestFactoryWrapper} wrapping the given request factory.
     *
     * @param wrappedFactory the request factory to be wrapped.
     * @param matchers the matchers used to enable/disable the rule.
     * @param failIfNotMatch true if the processing must fail if the matchers are not OK.
     */
    protected AbstractMfClientHttpRequestFactoryWrapper(
            final MfClientHttpRequestFactory wrappedFactory,
            final UriMatchers matchers, final boolean failIfNotMatch) {
        Assert.notNull(wrappedFactory, "'requestFactory' must not be null");
        Assert.notNull(matchers, "'matchers' must not be null");
        this.wrappedFactory = wrappedFactory;
        this.matchers = matchers;
        this.failIfNotMatch = failIfNotMatch;
    }


    /**
     * This implementation simply calls {@link #createRequest(URI, HttpMethod, MfClientHttpRequestFactory)}
     * (if the matchers are OK)  with the wrapped request factory provided to the {@linkplain
     * #AbstractMfClientHttpRequestFactoryWrapper(MfClientHttpRequestFactory, UriMatchers, boolean)
     * constructor}.
     *
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     */
    public final ClientHttpRequest createRequest(
            final URI uri,
            final HttpMethod httpMethod) throws IOException {
        if (uri.getScheme() == null || uri.getScheme().equals("file") ||
                this.matchers.matches(uri, httpMethod)) {
            return createRequest(uri, httpMethod, this.wrappedFactory);
        } else if (this.failIfNotMatch) {
            throw new IllegalArgumentException(uri + " is denied.");
        } else {
            return this.wrappedFactory.createRequest(uri, httpMethod);
        }
    }

    /**
     * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method by using the passed-on
     * request factory.
     * <p>Called from {@link #createRequest(URI, HttpMethod)}.
     *
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     * @param requestFactory the wrapped request factory
     * @return the created request
     * @throws IOException in case of I/O errors
     */
    protected abstract ClientHttpRequest createRequest(
            URI uri,
            HttpMethod httpMethod,
            MfClientHttpRequestFactory requestFactory) throws IOException;

    @Override
    public final void register(final RequestConfigurator callback) {
        this.wrappedFactory.register(callback);
    }
}
