package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Allows to check that a given URL matches a hostname literally (textual match).
 *
 * <p>Example 1: Accept any www.camptocamp.com url</p>
 * <pre><code>
 *     - !hostnameMatch
 *       host : www.camptocamp.com
 * </code></pre>
 * <p>Example 2: Accept any www.camptocamp.com url (port == -1 accepts any port)</p>
 * <pre><code>
 *     - !hostnameMatch
 *       host : www.camptocamp.com
 *       port : -1
 * </code></pre>
 * <p>Example 3: Accept any www.camptocamp.com url on port 80 only</p>
 * <pre><code>
 *     - !hostnameMatch
 *       host : www.camptocamp.com
 *       port : 80
 * </code></pre>
 *
 * Example 4: Accept www.camptocamp.com urls with paths that start with /print/.
 * <p>
 * If the regular expression give does not start with / then it will be added because all paths start with /
 * </p>
 *
 * <pre><code>
 *     - !hostnameMatch
 *       host : www.camptocamp.com
 *       pathRegex : /print/.+
 * </code></pre>
 */
public final class HostnameMatcher extends HostMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostnameMatcher.class);
    private String host;
    private boolean allowSubDomains = false;

    /* (non-Javadoc)
     * @see org.mapfish.print.config.ConfigurationObject#validate(java.util.List, org.mapfish.print.config
     * .Configuration)
     */
    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.host == null) {
            validationErrors.add(new ConfigurationException("No host defined: " + getClass().getName()));
        }
    }

    @Override
    protected Optional<Boolean> tryOverrideValidation(final MatchInfo matchInfo) {
        String host = matchInfo.getHost();
        if (host == MatchInfo.ANY_HOST) {
            return Optional.empty();
        }
        if (isHostnameMatch(host)) {
            return Optional.empty();
        }
        return Optional.of(false);
    }

    private boolean isHostnameMatch(final String host) {
        boolean match = this.host.equalsIgnoreCase(host);
        if (this.allowSubDomains && !match) {
            match = host.toLowerCase().endsWith("." + this.host);
        }
        return match;
    }

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("HostnameMatcher");
        sb.append("{host='").append(host).append('\'');
        sb.append(", allowSubDomains=").append(this.allowSubDomains);
        if (port >= 0) {
            sb.append(", port=").append(port);
        }
        if (pathRegex != null) {
            sb.append(", pathRegexp=").append(pathRegex);
        }
        sb.append(", reject=").append(isReject());
        sb.append('}');
        return sb.toString();
    }

    /**
     * Set the host.
     *
     * @param host the host
     */
    public void setHost(final String host) {
        this.host = host != null ? host.toLowerCase() : null;
    }

    /**
     * Set if sub-domains are allowed.
     *
     * @param allowSubDomains true if allowed
     */
    public void setAllowSubDomains(final boolean allowSubDomains) {
        this.allowSubDomains = allowSubDomains;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final HostnameMatcher that = (HostnameMatcher) o;
        return allowSubDomains == that.allowSubDomains &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, allowSubDomains);
    }
    // CHECKSTYLE:ON
}
