package org.mapfish.print.processor.http.matcher;

import com.google.common.base.Optional;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

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
 * <p></p>
 *     Example 4: Accept www.camptocamp.com urls with paths that start with /print/.
 *     <p>
 *         If the regular expression give does not start with / then it will be added because all paths start with /
 *     </p>
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

    /**
     * Creates a new instance.
     */
    public HostnameMatcher() {
        super();
    }

    /* (non-Javadoc)
     * @see org.mapfish.print.config.ConfigurationObject#validate(java.util.List, org.mapfish.print.config.Configuration)
     */
    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.host == null) {
            validationErrors.add(new ConfigurationException("No host defined: " + getClass().getName()));
        }
    }

    /* (non-Javadoc)
     * @see org.mapfish.print.processor.http.matcher.HostMatcher#tryOverrideValidation(org.mapfish.print.processor.http.matcher.MatchInfo)
     */
    @Override
    protected Optional<Boolean> tryOverrideValidation(final MatchInfo matchInfo)
            throws UnknownHostException, SocketException, MalformedURLException {
        String host = matchInfo.getHost();
        if (host == MatchInfo.ANY_HOST) {
            return Optional.absent();
        }
        if (isHostnameMatch(host)) {
            return Optional.absent();
        }
        return Optional.of(false);
    }

    /**
     * @param host the host
     * @return true, if the hostname matches.
     */
    protected boolean isHostnameMatch(final String host) {
        boolean match = this.host.equalsIgnoreCase(host);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Configured hostname '" + this.host + "' matches requested '" + host + "': " + match);
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
     * @param host the host
     */
    public void setHost(final String host) throws UnknownHostException {
        this.host = host;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HostnameMatcher other = (HostnameMatcher) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        return true;
    }
    // CHECKSTYLE:ON
}
