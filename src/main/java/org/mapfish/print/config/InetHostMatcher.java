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

import java.net.*;
import java.util.Arrays;

/**
 * Allows to check that a given URL matches an IP address (numeric format)
 */
public abstract class InetHostMatcher extends HostMatcher {
    public static final Logger LOGGER = Logger.getLogger(InetHostMatcher.class);

    protected byte[][] authorizedIPs = null;

    public boolean validate(URI uri) throws UnknownHostException, SocketException, MalformedURLException {
        final InetAddress maskAddress = getMaskAddress();
        final InetAddress[] requestedIPs;
        try {
            requestedIPs = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException ex) {
            return false;
        }
        boolean oneMatching = false;
        for (int i = 0; i < requestedIPs.length; ++i) {
            InetAddress requestedIP = requestedIPs[i];
            if (isInAuthorized(requestedIP, maskAddress)) {
                oneMatching = true;
                break;
            }
        }
        return oneMatching && super.validate(uri);
    }

    private boolean isInAuthorized(InetAddress requestedIP, InetAddress mask) throws UnknownHostException, SocketException {
        byte[] rBytes = mask(requestedIP, mask);
        final byte[][] authorizedIPs = getAuthorizedIPs(mask);
        for (int i = 0; i < authorizedIPs.length; ++i) {
            byte[] authorizedIP = authorizedIPs[i];
            if (compareIP(rBytes, authorizedIP)) {
                return true;
            }
        }
        LOGGER.debug("Address not in the authorizeds: " + requestedIP);
        return false;
    }

    private boolean compareIP(byte[] rBytes, byte[] authorizedIP) {
        if (rBytes.length != authorizedIP.length) {
            return false;
        }
        for (int j = 0; j < authorizedIP.length; ++j) {
            byte bA = authorizedIP[j];
            byte bR = rBytes[j];
            if (bA != bR) {
                return false;
            }
        }
        return true;
    }

    private byte[] mask(InetAddress address, InetAddress mask) {
        byte[] aBytes = address.getAddress();
        if (mask != null) {
            byte[] mBytes = mask.getAddress();
            if (aBytes.length != mBytes.length) {
                LOGGER.warn("Cannot mask address [" + address + "] with :" + mask);
                return aBytes;
            } else {
                final byte[] result = new byte[aBytes.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = (byte) (aBytes[i] & mBytes[i]);
                }
                return result;
            }
        } else {
            return aBytes;
        }
    }

    protected abstract InetAddress getMaskAddress() throws UnknownHostException;

    protected void buildMaskedAuthorizedIPs(InetAddress[] ips) throws UnknownHostException {
        final InetAddress maskAddress = getMaskAddress();
        authorizedIPs = new byte[ips.length][];
        for (int i = 0; i < ips.length; ++i) {
            authorizedIPs[i] = mask(ips[i], maskAddress);
        }
    }

    protected abstract byte[][] getAuthorizedIPs(InetAddress mask) throws UnknownHostException, SocketException;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(authorizedIPs);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        InetHostMatcher other = (InetHostMatcher) obj;
        if (!Arrays.equals(authorizedIPs, other.authorizedIPs))
            return false;
        return true;
    }
}
