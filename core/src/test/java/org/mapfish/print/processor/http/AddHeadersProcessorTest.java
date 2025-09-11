package org.mapfish.print.processor.http;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(
    locations = {
      "classpath:org/mapfish/print/processor/http/add-headers/add-custom-processor-application-context.xml"
    })
public class AddHeadersProcessorTest extends AbstractHttpProcessorTest {

  @Override
  protected String baseDir() {
    return "add-headers";
  }

  @Override
  protected Class<TestProcessor> testProcessorClass() {
    return TestProcessor.class;
  }

  @Override
  protected Class<? extends AbstractClientHttpRequestFactoryProcessor> classUnderTest() {
    return AddHeadersProcessor.class;
  }

  public static class TestProcessor extends AbstractTestProcessor {
    @Nullable
    @Override
    public Void execute(TestParam values, ExecutionContext context) throws Exception {
      matching(values);
      notMatching(values);
      return null;
    }

    private void matching(TestParam values) throws Exception {
      final URI uri = new URI("http://localhost:8080/path?query#fragment");
      final ClientHttpRequest request =
          values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      final URI finalUri = request.getURI();
      assertEquals(uri, finalUri);

      assertEquals(2, request.getHeaders().size());
      assertArrayEquals(
          new Object[] {"cookie-value", "cookie-value2"},
          request.getHeaders().get("Cookie").toArray());
      assertArrayEquals(
          new Object[] {"header2-value"}, request.getHeaders().get("Header2").toArray());
    }

    private void notMatching(TestParam values) throws Exception {
      final URI uri = new URI("http://195.176.255.226:8080/path?query#fragment");
      final ClientHttpRequest request =
          values.clientHttpRequestFactoryProvider.get().createRequest(uri, HttpMethod.GET);
      final URI finalUri = request.getURI();
      assertEquals(uri, finalUri);

      assertEquals(0, request.getHeaders().size());
    }
  }
}
