package org.mapfish.print.http;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

public class MfClientHttpRequestFactoryImplTest {
  private static final int TARGET_PORT = 33212;
  private static HttpServer targetServer;

  @BeforeClass
  public static void setUp() throws Exception {
    targetServer =
        HttpServer.create(new InetSocketAddress(HttpProxyTest.LOCALHOST, TARGET_PORT), 0);
    targetServer.start();
  }

  @AfterClass
  public static void tearDown() {
    targetServer.stop(0);
  }

  @Test
  public void testGetHeaders() throws Exception {
    targetServer.createContext(
        "/request",
        httpExchange -> {
          final Headers responseHeaders = httpExchange.getResponseHeaders();
          responseHeaders.add("Content-Type", "application/json; charset=utf8");
          httpExchange.sendResponseHeaders(200, 0);
          httpExchange.close();
        });

    MfClientHttpRequestFactoryImpl factory =
        new MfClientHttpRequestFactoryImpl(20, 10, new ThreadPoolJobManager());
    final ConfigurableRequest request =
        factory.createRequest(
            new URI("http://" + HttpProxyTest.LOCALHOST + ":" + TARGET_PORT + "/request"),
            HttpMethod.GET);

    try (ClientHttpResponse response = request.execute()) {
      assertEquals(
          "application/json; charset=utf8", response.getHeaders().getFirst("Content-Type"));
    }
  }
}
