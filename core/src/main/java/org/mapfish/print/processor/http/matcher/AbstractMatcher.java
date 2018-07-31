package org.mapfish.print.processor.http.matcher;

/**
 * Base matcher for checking that URIs are authorized or denied.
 */
public abstract class AbstractMatcher implements URIMatcher {
    private boolean reject = false;

    public final boolean isReject() {
        return this.reject;
    }

    /**
     * Reverses the matcher. Instead of accepting an URI when the URI matches, the URI is rejected.
     *
     * @param reject Should reject?
     */
    public final void setReject(final boolean reject) {
        this.reject = reject;
    }
}
