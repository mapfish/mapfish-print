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

package org.mapfish.print.config;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Compares ip address string and mask string by using {@link java.net.InetAddress} comparison.
 */
public class AddressHostMatcher extends InetHostMatcher {
    private String ip = null;
    private String mask = null;

    private InetAddress maskAddress = null;

    @Override
    protected byte[][] getAuthorizedIPs(final InetAddress mask) throws UnknownHostException, SocketException {
        if (authorizedIPs == null) {
            InetAddress[] ips = InetAddress.getAllByName(ip);
            this.authorizedIPs = buildMaskedAuthorizedIPs(ips);
        }
        return authorizedIPs;
    }

    @Override
    protected InetAddress getMaskAddress() throws UnknownHostException {
        if (this.maskAddress == null && this.mask != null) {
            this.maskAddress = InetAddress.getByName(this.mask);
        }
        return this.maskAddress;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public void setMask(final String mask) {
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