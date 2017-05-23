package org.mapfish.print.url.data;

import org.geotools.data.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Decodes the base64 data and provides an appropriate InputStream.
 */
public class DataUrlConnection extends URLConnection {

    /**
     * Must be overridden.
     * 
     * @param url the data url
     */
    protected DataUrlConnection(final URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        // nothing to be done
    }

    @Override
    public InputStream getInputStream() {
        String url = this.url.toExternalForm();
        url = url.substring(url.lastIndexOf(",") + 1);
        return new ByteArrayInputStream(Base64.decode(url));
    }

}
