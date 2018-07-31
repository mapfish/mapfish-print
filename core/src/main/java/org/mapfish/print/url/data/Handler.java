package org.mapfish.print.url.data;

import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Responsible for instantiating the DataUrlConnection.
 */
public class Handler extends URLStreamHandler {

    /**
     * Adds the parent package to the java.protocol.handler.pkgs system property.
     */
    public static void configureProtocolHandler() {
        final String pkgs = System.getProperty("java.protocol.handler.pkgs");
        String newValue = "org.mapfish.print.url";
        if (pkgs != null && !pkgs.contains(newValue)) {
            newValue = newValue + "|" + pkgs;
        } else if (pkgs != null) {
            newValue = pkgs;
        }
        System.setProperty("java.protocol.handler.pkgs", newValue);
    }

    @Override
    protected URLConnection openConnection(final URL url) {
        return new DataUrlConnection(url);
    }

    @Override
    protected URLConnection openConnection(final URL url, final Proxy proxy) {
        return new DataUrlConnection(url);
    }
}
