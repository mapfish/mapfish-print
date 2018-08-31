package org.mapfish.print.processor.http.matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * Hold a list of {@link URIMatcher} and implement the logic to see if any matches an URI.
 */
public final class UriMatchers {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriMatchers.class);
    private List<? extends URIMatcher> matchers;

    /**
     * Constructor.
     */
    public UriMatchers() {
        matchers = Collections.singletonList(new AcceptAllMatcher());
    }

    /**
     * Constructor.
     *
     * @param matchers the list
     */
    public UriMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers = matchers;
    }

    /**
     * Set the matchers.
     *
     * @param matchers the new list.
     */
    public void setMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers = matchers;
    }

    /**
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     * @return true if it's matching.
     */
    public boolean matches(final URI uri, final HttpMethod httpMethod)
            throws SocketException, UnknownHostException, MalformedURLException {
        final MatchInfo matchInfo = MatchInfo.fromUri(uri, httpMethod);
        return matches(matchInfo);
    }

    /**
     * @param matchInfo The info to check for matches.
     * @return true if it's matching.
     */
    public boolean matches(final MatchInfo matchInfo)
            throws SocketException, UnknownHostException, MalformedURLException {
        for (URIMatcher matcher: this.matchers) {
            if (matcher.matches(matchInfo)) {
                if (matcher.isReject()) {
                    LOGGER.debug("Reject {} because of this rule: {}", matchInfo, matcher);
                    return false;
                } else {
                    LOGGER.debug("Accept {} because of this rule: {}", matchInfo, matcher);
                    return true;
                }
            }
        }
        LOGGER.debug("Reject {} because no rule matches", matchInfo);
        return false;
    }

    /**
     * Validate the configuration.
     *
     * @param validationErrors where to put the errors.
     */
    public void validate(final List<Throwable> validationErrors) {
        if (this.matchers == null) {
            validationErrors.add(new IllegalArgumentException(
                    "Matchers cannot be null.  There should be at least a !acceptAll matcher"));
        }
        if (this.matchers != null && this.matchers.isEmpty()) {
            validationErrors.add(new IllegalArgumentException(
                    "There are no url matchers defined.  There should be at least a " +
                            "!acceptAll matcher"));
        }
    }
}
