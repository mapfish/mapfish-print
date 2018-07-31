package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.ConfigurationObject;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Checks if a uri is a permitted uri.
 */
public interface URIMatcher extends ConfigurationObject {
    /**
     * Check if the uri is matching.
     *
     * @param matchInfo the matching information to check
     * @return true if the uri is matching or false otherwis
     */
    boolean matches(MatchInfo matchInfo)
            throws UnknownHostException, SocketException, MalformedURLException;

    /**
     * If true and the matcher accepts the uri, the request needs to be rejected.
     */
    boolean isReject();
}
