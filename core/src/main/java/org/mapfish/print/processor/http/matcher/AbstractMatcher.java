package org.mapfish.print.processor.http.matcher;

/**
 * Base matcher for checking that URIs are authorized or denied.
 */
public abstract class AbstractMatcher implements URIMatcher {
    private boolean reject = false;

    public final boolean isReject() {
        return this.reject;
    }

    public final void setReject(final boolean reject) {
        this.reject = reject;
    }
}
