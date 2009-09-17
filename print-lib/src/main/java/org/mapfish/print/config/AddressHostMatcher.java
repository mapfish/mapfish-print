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

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AddressHostMatcher extends InetHostMatcher {
    private String ip = null;
    private String mask = null;

    private InetAddress maskAddress = null;

    protected byte[][] getAuthorizedIPs(InetAddress mask) throws UnknownHostException, SocketException {
        if (authorizedIPs == null) {
            InetAddress[] ips = InetAddress.getAllByName(ip);
            buildMaskedAuthorizedIPs(ips);
        }
        return authorizedIPs;
    }

    protected InetAddress getMaskAddress() throws UnknownHostException {
        if (maskAddress == null && mask != null) {
            maskAddress = InetAddress.getByName(mask);
        }
        return maskAddress;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }


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
}