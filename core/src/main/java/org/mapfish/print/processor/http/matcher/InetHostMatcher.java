package org.mapfish.print.processor.http.matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Allows to check that a given URL matches an IP address (numeric format).
 */
public abstract class InetHostMatcher extends HostMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(InetHostMatcher.class);

    private List<AddressMask> authorizedIPs = null;

    private static byte[] mask(final byte[] address, final byte[] mask) {
        if (mask != null) {
            if (address.length != mask.length) {
                LOGGER.warn("Cannot mask address [{}] with: {}", Arrays.toString(address),
                            Arrays.toString(mask));
                return address;
            } else {
                final byte[] result = new byte[address.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = (byte) (address[i] & mask[i]);
                }
                return result;
            }
        } else {
            return address;
        }
    }

    @Override
    protected final Optional<Boolean> tryOverrideValidation(final MatchInfo matchInfo)
            throws UnknownHostException, SocketException {
        final String host = matchInfo.getHost();
        if (host == MatchInfo.ANY_HOST) {
            return Optional.empty();
        }

        final InetAddress[] requestedIPs;
        try {
            requestedIPs = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            return Optional.of(false);
        }
        for (InetAddress requestedIP: requestedIPs) {
            if (isInAuthorized(requestedIP)) {
                return Optional.empty();
            }
        }
        return Optional.of(false);
    }

    private boolean isInAuthorized(final InetAddress requestedIP) throws UnknownHostException,
            SocketException {
        final List<AddressMask> finalAuthorizedIPs = getAuthorizedIPs();
        final byte[] address = requestedIP.getAddress();
        for (AddressMask authorizedIP: finalAuthorizedIPs) {
            if (compareIP(address, authorizedIP)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareIP(final byte[] requestedIP, final AddressMask authorizedIP) {
        if (requestedIP.length != authorizedIP.address.length) {
            return false;
        }
        byte[] maskedRequest = mask(requestedIP, authorizedIP.mask);
        return Arrays.equals(authorizedIP.address, maskedRequest);
    }

    private List<AddressMask> getAuthorizedIPs() throws SocketException, UnknownHostException {
        if (this.authorizedIPs == null) {
            this.authorizedIPs = createAuthorizedIPs();
        }
        return this.authorizedIPs;
    }

    /**
     * Get the full list of authorized IP addresses and the masks.
     */
    protected abstract List<AddressMask> createAuthorizedIPs() throws UnknownHostException, SocketException;

    /**
     * Reset the authorized IPs cache.
     */
    protected final void clearAuthorizedIPs() {
        this.authorizedIPs = null;
    }

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + authorizedIPs.hashCode();
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
        InetHostMatcher other = (InetHostMatcher) obj;
        return authorizedIPs.equals(other.authorizedIPs);
    }

    /**
     * The ip addresses that are considered legal.
     */
    protected static class AddressMask {
        private final byte[] address;
        @Nullable
        private final byte[] mask;

        /**
         * IP and mask are given.
         *
         * @param ip The IP address
         * @param mask A null mask means match all.
         */
        public AddressMask(final InetAddress ip, final InetAddress mask) {
            this.mask = mask != null ? mask.getAddress() : null;
            this.address = mask(ip.getAddress(), this.mask);
        }

        /**
         * Guess the mask in function of the address: /8 for IPv4 loopback and full match for the rest.
         *
         * @param address The IP address
         */
        public AddressMask(final InetAddress address) {
            if (address.isLoopbackAddress() && address instanceof Inet4Address) {
                final byte all = (byte) 0xff;
                this.mask = new byte[]{all, 0, 0, 0};
            } else {
                this.mask = null;
            }
            this.address = mask(address.getAddress(), this.mask);
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            return Arrays.hashCode(this.address) * prime + Arrays.hashCode(this.mask);
        }

        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AddressMask other = (AddressMask) obj;
            return Arrays.equals(this.address, other.address) && Arrays.equals(this.mask, other.mask);
        }
    }
    // CHECKSTYLE:ON

}
