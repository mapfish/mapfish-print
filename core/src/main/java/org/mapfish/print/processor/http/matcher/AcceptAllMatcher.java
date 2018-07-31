package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;

import java.util.List;

/**
 * A matcher that always returns true. [[examples=http_processors]]
 */
public final class AcceptAllMatcher extends AbstractMatcher {
    /**
     * A singleton instance to reduce creating unnecessary instances.
     */
    public static final AcceptAllMatcher INSTANCE = new AcceptAllMatcher();

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    @Override
    public String toString() {
        return "Accept All";
    }

    @Override
    public boolean matches(final MatchInfo matchInfo) {
        return true;
    }
}
