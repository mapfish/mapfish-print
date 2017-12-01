package org.mapfish.print.url.data;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

public class TestHandler {
    @Test
    public void testBase64() throws IOException {
        Handler.configureProtocolHandler();
        final URL url = new URL("data:text/plain;base64,SGVsbG8gd29ybGQ=");
        final InputStream content = (InputStream) url.getContent();
        assertEquals("Hello world", IOUtils.toString(content, "utf-8"));
    }

    @Test
    public void testText() throws IOException {
        Handler.configureProtocolHandler();
        final URL url = new URL("data:text/plain;charset=utf-8,HelloWorld");
        final InputStream content = (InputStream) url.getContent();
        assertEquals("HelloWorld", IOUtils.toString(content, "utf-8"));
    }
}
