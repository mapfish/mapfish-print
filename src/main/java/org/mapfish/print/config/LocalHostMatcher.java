/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Allows to check that a given URL is served by one of the local network
 * interface or one of its alias.
 */
public class LocalHostMatcher extends InetHostMatcher {

    protected byte[][] getAuthorizedIPs(InetAddress mask) throws UnknownHostException, SocketException {
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

            buildMaskedAuthorizedIPs(result);
        }
        return authorizedIPs;
    }

    public String toString() {
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

    protected InetAddress getMaskAddress() throws UnknownHostException {
        return null;
    }

    @SuppressWarnings({"EmptyMethod"})
    public void setDummy(boolean dummy) {
        //YAML parser always need some content
    }
}
