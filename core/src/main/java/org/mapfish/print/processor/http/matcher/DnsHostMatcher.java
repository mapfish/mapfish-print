package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Allows to check that a given URL matches a DNS address (textual format). The actual comparison is performed
 * on numerical IP addresses. The configured DNS host address is transformed into numerical IP addresses
 * during application startup. The urls to be compared are transformed during the print processing.
 * <p>Example 1: Accept any www.camptocamp.com url</p>
 * <pre><code>
 *     - !dnsMatch
 *       host : www.camptocamp.com
 * </code></pre>
 * <p>Example 2: Accept any www.camptocamp.com url (port == -1 accepts any port)</p>
 * <pre><code>
 *     - !dnsMatch
 *       host : www.camptocamp.com
 *       port : -1
 * </code></pre>
 * <p>Example 3: Accept any www.camptocamp.com url on port 80 only</p>
 * <pre><code>
 *     - !dnsMatch
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
 *     - !dnsMatch
 *       host : www.camptocamp.com
 *       pathRegex : /print/.+
 * </code></pre>
 * [[examples=http_processors]]
 */
public class DnsHostMatcher extends HostMatcher {
    private List<AddressHostMatcher> matchersForHost = new ArrayList<>();
    private String host;

    /**
     * Check the given URI to see if it matches.
     *
     * @param matchInfo the matchInfo to validate.
     * @return True if it matches.
     */
    @Override
    public final Optional<Boolean> tryOverrideValidation(final MatchInfo matchInfo) throws SocketException,
            UnknownHostException, MalformedURLException {
        for (AddressHostMatcher addressHostMatcher: this.matchersForHost) {
            if (addressHostMatcher.matches(matchInfo)) {
                return Optional.empty();
            }
        }

        return Optional.of(false);
    }

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.host == null) {
            validationErrors.add(new ConfigurationException("No host defined: " + getClass().getName()));
        }
    }

    /**
     * Set the host.
     *
     * @param host the host
     */
    public final void setHost(final String host) throws UnknownHostException {
        this.host = host;
        final InetAddress[] inetAddresses = InetAddress.getAllByName(host);

        for (InetAddress address: inetAddresses) {
            final AddressHostMatcher matcher = new AddressHostMatcher();
            matcher.setIp(address.getHostAddress());
            this.matchersForHost.add(matcher);
        }
    }

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DnsHostMatcher");
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
        DnsHostMatcher other = (DnsHostMatcher) obj;
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
