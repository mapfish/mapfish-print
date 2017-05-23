package org.mapfish.print.url.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Responsible for instantiating the DataUrlConnection.
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new DataUrlConnection(url);
    }

}
