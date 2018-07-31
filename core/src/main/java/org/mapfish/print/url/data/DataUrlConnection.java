package org.mapfish.print.url.data;

import org.apache.http.entity.ContentType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

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
    public void connect() {
        // nothing to be done
    }

    @Override
    public InputStream getInputStream() {
        final String url = this.url.toExternalForm();
        final String data = url.substring(url.lastIndexOf(",") + 1);

        final String fullContentType = getFullContentType();
        if (fullContentType.endsWith(";base64")) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(data));
        } else {
            final ContentType contentType = ContentType.parse(fullContentType);
            Charset charset = contentType.getCharset();
            if (charset == null) {
                charset = Charset.forName("UTF-8");
            }
            byte[] bytes = charset.encode(data).array();
            if (bytes[bytes.length - 1] == 0) {
                bytes = Arrays.copyOf(bytes, bytes.length - 1);
            }
            return new ByteArrayInputStream(bytes);
        }
    }

    @Override
    public String getContentType() {
        return getFullContentType().replace(";base64,", ",");
    }

    /**
     * Get the content-type, including the optional ";base64".
     */
    public String getFullContentType() {
        final String url = this.url.toExternalForm().substring("data:".length());
        final int endIndex = url.indexOf(',');
        if (endIndex >= 0) {
            final String contentType = url.substring(0, endIndex);
            if (!contentType.isEmpty()) {
                return contentType;
            }
        }
        return "text/plain;charset=US-ASCII";
    }
}
