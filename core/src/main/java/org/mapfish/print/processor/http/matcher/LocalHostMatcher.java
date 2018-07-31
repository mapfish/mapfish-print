package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.Configuration;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Allows to check that a given URL is served by one of the local network interface or one of its aliases.
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
 * <p>Example 4: Accept localhost urls with paths that start with /print/.</p>
 * <p>
 * If the regular expression given does not start with / then it will be added because all paths start with
 * /.
 * </p>
 * <pre><code>
 *     - localMatch
 *       pathRegex : /print/.+
 * </code></pre>
 * [[examples=http_processors]]
 */
public class LocalHostMatcher extends InetHostMatcher {

    @Override
    protected final List<AddressMask> createAuthorizedIPs() throws SocketException {
        List<AddressMask> authorizedIPs = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface networkInterface = ifaces.nextElement();
            final List<InterfaceAddress> addrs = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress netAddr: addrs) {
                authorizedIPs.add(new AddressMask(netAddr.getAddress()));
            }
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
        sb.append(", reject=").append(isReject());
        sb.append('}');
        return sb.toString();
    }
}
