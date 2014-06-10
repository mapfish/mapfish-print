package org.mapfish.print.processor.jasper.legends;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Custom URL protocol handler for files in folder "/map-data/legends".
 * Allows URLs like "legends:/points-de-vente.png"
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        final URL resourceUrl = getClass().getResource("/map-data/legends" + u.getFile());
        return resourceUrl.openConnection();
    }
}
