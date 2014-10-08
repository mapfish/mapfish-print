/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Compares ip address string and mask string by using {@link java.net.InetAddress} comparison.
 * <p>Example 1: accept any uri whose host matches the ip of www.camptocamp.com</p>
 * <pre><code>
 *     - !ipMatch
 *       ip : www.camptocamp.com
 * </code></pre>
 * <p>Example 2: accept any uri whose host ip starts with 192.1</p>
 * <pre><code>
 *     - !ipMatch
 *       ip : 192.1.0.0
 *       mask : 255.255.0.0
 * </code></pre>
 * <p>Example 3: accept any uri whose host ip starts with 192.1 and restricts to port 80</p>
 * <pre><code>
 *     - !ipMatch
 *       ip : 192.1.0.0
 *       mask : 255.255.0.0
 *       port : 80
 * </code></pre>
 * <p>Example 4: accept any uri whose host ip starts with 192.1 and and allows any port (-1 is any port)</p>
 * <pre><code>
 *     - !ipMatch
 *       ip : 192.1.0.0
 *       mask : 255.255.0.0
 *       port : -1
 * </code></pre>
 * <p>Example 5: accept any uri whose host ip starts with 192.1 and restricts to paths that start with /print/</p>
 * <pre><code>
 *     - !ipMatch
 *       ip : 192.1.0.0
 *       mask : 255.255.0.0
 *       pathRegex : /print/.+
 * </code></pre>
 */
public class AddressHostMatcher extends InetHostMatcher {
    private String ip = null;
    private String mask = null;

    private InetAddress maskAddress = null;

    @Override
    protected final byte[][] getAuthorizedIPs(final InetAddress maskForCalculation) throws UnknownHostException, SocketException {
        if (this.authorizedIPs == null) {
            InetAddress[] ips = InetAddress.getAllByName(this.ip);
            this.authorizedIPs = buildMaskedAuthorizedIPs(ips);
        }
        return authorizedIPs;
    }

    @Override
    protected final InetAddress getMaskAddress() throws UnknownHostException {
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
     * @param ip the ip address.
     */
    public final void setIp(final String ip) {
        this.authorizedIPs = null;
        this.ip = ip;
    }

    /**
     * Set the Mask to apply to the ip address obtained from the URI that is being tested.
     *
     * @param mask the mask ip address.
     */
    public final void setMask(final String mask) {
        this.maskAddress = null;
        this.mask = mask;
    }

    // Don't use checkstyle on generated methods
    // CHECKSTYLE:OFF
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
    // CHECKSTYLE:ON
}
