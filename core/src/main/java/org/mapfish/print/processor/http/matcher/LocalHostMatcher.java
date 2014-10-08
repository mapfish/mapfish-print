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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Allows to check that a given URL is served by one of the local network
 * interface or one of its aliases.
 * <p>Example 1: Accept any localhost url</p>
 * <pre><code>
 *     - localMatch {}
 * </code></pre>
 * <p>Example 2: Accept any localhost url (port == -1 accepts any port)</p>
 * <pre><code>
 *     - localMatch
 *       port : -1
 * </code></pre>
 * <p>Example 3: Accept any localhost url on port 80 only</p>
 * <pre><code>
 *     - localMatch
 *       port : 80
 * </code></pre>
 * <p>
 *     Example 4: Accept localhost urls with paths that start with /print/.
 *     <p>
 *         If the regular expression give does not start with / then it will be added because all paths start with /
 *     </p>
 * </p>
 * <pre><code>
 *     - localMatch
 *       pathRegex : /print/.+
 * </code></pre>
 */
public class LocalHostMatcher extends InetHostMatcher {

    @Override
    protected final byte[][] getAuthorizedIPs(final InetAddress mask) throws UnknownHostException, SocketException {
        if (authorizedIPs == null) {
            InetAddress[] result;
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
            while (ifaces.hasMoreElements()) {
                NetworkInterface networkInterface = ifaces.nextElement();
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    addresses.add(addrs.nextElement());
                }
            }
            result = addresses.toArray(new InetAddress[addresses.size()]);

            this.authorizedIPs = buildMaskedAuthorizedIPs(result);
        }
        return authorizedIPs;
    }

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }


    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LocalHostMatcher");
        sb.append("{");
        if (port >= 0) {
            sb.append("port=").append(port);
        }
        if (pathRegex != null) {
            sb.append(", pathRegexp=").append(pathRegex);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    protected final InetAddress getMaskAddress() throws UnknownHostException {
        return null;
    }
}
