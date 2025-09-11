package org.mapfish.print.processor.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(
    locations = {
      "classpath:org/mapfish/print/processor/http/use-http-for-https/add-custom-processor-application"
          + "-context.xml"
    })
public class UseHttpForHttpsProcessorTest extends AbstractHttpProcessorTest {

  @Override
  protected String baseDir() {
    return "use-http-for-https";
  }

  @Override
  protected Class<TestProcessor> testProcessorClass() {
    return TestProcessor.class;
  }

  @Override
  protected Class<? extends AbstractClientHttpRequestFactoryProcessor> classUnderTest() {
    return UseHttpForHttpsProcessor.class;
  }

  public static class TestProcessor extends AbstractTestProcessor {

    String userinfo = "user:pass";
    String host = "localhost";
    String path = "path";
    String query = "query";
    String fragment = "fragment";

    @Nullable
    @Override
    public Void execute(TestParam values, ExecutionContext context) throws Exception {
      testDefinedPortMapping(values);
      testImplicitPortMapping(values);
      testUriWithOnlyAuthoritySegment(values);
      testHttp(values);
      return null;
    }

    private void testUriWithOnlyAuthoritySegment(TestParam values)
        throws URISyntaxException, IOException {
      String authHost = "center_wmts_fixedscale.com";

      URI uri = new URI("https://" + userinfo + "@" + authHost + ":8443/" + path);
      ClientHttpRequest request =
          values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(userinfo + "@" + authHost + ":9999", request.getURI().getAuthority());
      assertEquals("/" + path, request.getURI().getPath());

      uri = new URI("https://" + authHost + ":8443/" + path);
      request = values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(authHost + ":9999", request.getURI().getAuthority());

      uri = new URI("https://" + authHost + "/" + path);
      request = values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(authHost, request.getURI().getAuthority());

      uri = new URI("https://" + userinfo + "@" + authHost + "/" + path);
      request = values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(userinfo + "@" + authHost, request.getURI().getAuthority());
    }

    private void testHttp(TestParam values) throws URISyntaxException, IOException {
      String uriString =
          String.format("http://%s@%s:9999/%s?%s#%s", userinfo, host, path, query, fragment);
      final ClientHttpRequest request =
          values
              .clientHttpRequestFactoryProvider
              .get()
              .createRequest(new URI(uriString), HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(userinfo, request.getURI().getUserInfo());
      assertEquals(host, request.getURI().getHost());
      assertEquals(9999, request.getURI().getPort());
      assertEquals("/" + path, request.getURI().getPath());
      assertEquals(query, request.getURI().getQuery());
      assertEquals(fragment, request.getURI().getFragment());
    }

    private void testDefinedPortMapping(TestParam values) throws IOException, URISyntaxException {
      String uriString =
          String.format("https://%s@%s:8443/%s?%s#%s", userinfo, host, path, query, fragment);
      final ClientHttpRequest request =
          values
              .clientHttpRequestFactoryProvider
              .get()
              .createRequest(new URI(uriString), HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(userinfo, request.getURI().getUserInfo());
      assertEquals(host, request.getURI().getHost());
      assertEquals(9999, request.getURI().getPort());
      assertEquals("/" + path, request.getURI().getPath());
      assertEquals(query, request.getURI().getQuery());
      assertEquals(fragment, request.getURI().getFragment());
    }

    private void testImplicitPortMapping(TestParam values) throws IOException, URISyntaxException {
      String uriString =
          String.format("https://%s@%s/%s?%s#%s", userinfo, host, path, query, fragment);
      final ClientHttpRequest request =
          values
              .clientHttpRequestFactoryProvider
              .get()
              .createRequest(new URI(uriString), HttpMethod.GET);
      assertEquals("http", request.getURI().getScheme());
      assertEquals(userinfo, request.getURI().getUserInfo());
      assertEquals(host, request.getURI().getHost());
      assertEquals(-1, request.getURI().getPort());
      assertEquals("/" + path, request.getURI().getPath());
      assertEquals(query, request.getURI().getQuery());
      assertEquals(fragment, request.getURI().getFragment());
    }
  }
}
