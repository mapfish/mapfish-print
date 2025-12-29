package org.mapfish.print.url.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class TestHandler {
  @Test
  public void testBase64() throws IOException, URISyntaxException {
    Handler.configureProtocolHandler();
    final URL url = URL.of(new URI("data:text/plain;base64,SGVsbG8gd29ybGQ="), null);
    final InputStream content = (InputStream) url.getContent();
    assertEquals("Hello world", IOUtils.toString(content, StandardCharsets.UTF_8));
  }

  @Test
  public void testText() throws IOException, URISyntaxException {
    Handler.configureProtocolHandler();
    final URL url = URL.of(new URI("data:text/plain;charset=utf-8,HelloWorld"), null);
    final InputStream content = (InputStream) url.getContent();
    assertEquals("HelloWorld", IOUtils.toString(content, StandardCharsets.UTF_8));
  }
}
