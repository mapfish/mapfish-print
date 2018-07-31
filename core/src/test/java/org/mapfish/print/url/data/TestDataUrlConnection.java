package org.mapfish.print.url.data;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

public class TestDataUrlConnection {
    @Test
    public void testGetContentType() throws MalformedURLException {
        Handler.configureProtocolHandler();
        assertEquals("image/svg+xml;base64",
                     new DataUrlConnection(new URL("data:image/svg+xml;base64,PHN2")).getContentType());
        assertEquals("image/svg+xml",
                     new DataUrlConnection(new URL("data:image/svg+xml,PHN2")).getContentType());
        assertEquals("text/plain;charset=US-ASCII",
                     new DataUrlConnection(new URL("data:,PHN2")).getContentType());
    }
}
