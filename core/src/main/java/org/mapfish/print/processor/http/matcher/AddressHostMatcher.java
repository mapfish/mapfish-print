package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares ip address string and mask string by using {@link java.net.InetAddress} comparison.
 * <p>Example 1: accept any uri whose host matches the ip of www.camptocamp.com</p>
 * <pre><code>
 *     - !ipMatch
 *       ip: www.camptocamp.com
 * </code></pre>
 * <p>Example 2: accept any uri whose host ip starts with 192.1</p>
 * <pre><code>
 *     - !ipMatch
 *       ip: 192.1.0.0
 *       mask: 255.255.0.0
 * </code></pre>
 * <p>Example 3: accept any uri whose host ip starts with 192.1 and restricts to port 80</p>
 * <pre><code>
 *     - !ipMatch
 *       ip: 192.1.0.0
 *       mask: 255.255.0.0
 *       port: 80
 * </code></pre>
 * <p>Example 4: accept any uri whose host ip starts with 192.1 and and allows any port (-1 is any port)</p>
 * <pre><code>
 *     - !ipMatch
 *       ip: 192.1.0.0
 *       mask: 255.255.0.0
 *       port: -1
 * </code></pre>
 * <p>Example 5: accept any uri whose host ip starts with 192.1 and restricts to paths that start with
 * /print/</p>
 * <pre><code>
 *     - !ipMatch
 *       ip: 192.1.0.0
 *       mask: 255.255.0.0
 *       pathRegex: /print/.+
 * </code></pre>
 * [[examples=http_processors]]
 */
public class AddressHostMatcher extends InetHostMatcher {
    private String ip = null;
    private String mask = null;

    private InetAddress maskAddress = null;

    @Override
    protected final List<AddressMask> createAuthorizedIPs() throws UnknownHostException {
        InetAddress[] ips = InetAddress.getAllByName(this.ip);
        final ArrayList<AddressMask> authorizedIPs = new ArrayList<>(ips.length);
        final InetAddress theMask = getMaskAddress();
        for (InetAddress actualIp: ips) {
            authorizedIPs.add(new AddressMask(actualIp, theMask));
        }
        return authorizedIPs;
    }

    private InetAddress getMaskAddress() throws UnknownHostException {
        if (this.maskAddress == null && this.mask != null) {
            this.maskAddress = InetAddress.getByName(this.mask);
        }
        return this.maskAddress;
    }

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.ip == null) {
            validationErrors.add(new ConfigurationException("No IP address defined " + getClass().getName()));
        }
    }

    /**
     * Set the allowed ip address for this matcher.
     *
     * @param ip the ip address.
     */
    public final void setIp(final String ip) {
        clearAuthorizedIPs();
        this.ip = ip;
    }

    /**
     * Set the Mask to apply to the ip address obtained from the URI that is being tested.
     *
     * @param mask the mask ip address.
     */
    public final void setMask(final String mask) {
        clearAuthorizedIPs();
        this.maskAddress = null;
        this.mask = mask;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AddressHostMatcher");
        sb.append("{ip='").append(ip).append('\'');
        if (mask != null) {
            sb.append(", mask='").append(mask).append('\'');
        }
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
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result + ((mask == null) ? 0 : mask.hashCode());
        result = prime * result + ((maskAddress == null) ? 0 : maskAddress.hashCode());
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
        AddressHostMatcher other = (AddressHostMatcher) obj;
        if (ip == null) {
            if (other.ip != null) {
                return false;
            }
        } else if (!ip.equals(other.ip)) {
            return false;
        }
        if (mask == null) {
            if (other.mask != null) {
                return false;
            }
        } else if (!mask.equals(other.mask)) {
            return false;
        }
        if (maskAddress == null) {
            if (other.maskAddress != null) {
                return false;
            }
        } else if (!maskAddress.equals(other.maskAddress)) {
            return false;
        }
        return true;
    }
}
