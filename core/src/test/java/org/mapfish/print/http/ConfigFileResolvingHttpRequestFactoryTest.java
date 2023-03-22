package org.mapfish.print.http;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.url.data.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigFileResolvingHttpRequestFactoryTest extends AbstractMapfishSpringTest {

  static {
    Handler.configureProtocolHandler();
  }

  private static final String BASE_DIR = "/org/mapfish/print/servlet/";
  private static final String HOST = "host.com";

  final File logbackXml = getFile("/logback.xml");
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;

  private ConfigFileResolvingHttpRequestFactory resolvingFactory;

  @Before
  public void setUp() throws Exception {
    requestFactory.registerHandler(input -> true, createFileHandler(URI::getPath));

    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    this.resolvingFactory =
        new ConfigFileResolvingHttpRequestFactory(this.requestFactory, config, "test");
  }

  @Test
  public void testCreateRequestServlet() throws Exception {
    final String path = BASE_DIR + "requestData.json";
    final URI uri = new URI("servlet://" + path);
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

    final ClientHttpResponse response = request.execute();

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String expected = getFileContent(path);
    final String actual =
        new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(expected, actual);
  }

  @Test
  public void testCreateRequestHttpGet() throws Exception {
    final URI uri = new URI("http://" + HOST + ".test/logback.xml");
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);
    final ClientHttpResponse response = request.execute();
    final String actual =
        new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(getExpected(), actual);
  }

  private String getExpected() throws IOException {
    return new String(Files.readAllBytes(logbackXml.toPath()), Constants.DEFAULT_CHARSET);
  }

  @Test
  public void testCreateRequestHttpPost() throws Exception {
    URI uri = new URI("http://" + HOST + ".test/logback.xml");
    ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.POST);
    ClientHttpResponse response = request.execute();
    String actual = new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(getExpected(), actual);

    uri = logbackXml.toURI();
    request = resolvingFactory.createRequest(uri, HttpMethod.POST);
    response = request.execute();

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  public void testCreateRequestHttpWriteToBody() throws Exception {

    URI uri = new URI("http://" + HOST + ".test/logback.xml");
    ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);
    request.getBody().write(new byte[] {1, 2, 3});
    ClientHttpResponse response = request.execute();
    String actual = new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(getExpected(), actual);

    uri = logbackXml.toURI();
    request = resolvingFactory.createRequest(uri, HttpMethod.GET);
    request.getBody().write(new byte[] {1, 2, 3});
    response = request.execute();

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  public void testCreateRequestFile() throws Exception {
    final String path = BASE_DIR + "requestData.json";
    final URI uri = getFile(path).toURI();
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

    final ClientHttpResponse response = request.execute();

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String expected = getFileContent(path);
    final String actual =
        new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(expected, actual);
  }

  @Test
  public void testCreateRequestRelativeFileToConfig() throws Exception {
    final String path = BASE_DIR + "requestData.json";
    final URI uri = new URI("file://requestData.json");
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

    final ClientHttpResponse response = request.execute();

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String expected = getFileContent(path);
    final String actual =
        new String(IOUtils.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
    assertEquals(expected, actual);
  }

  @Test(expected = IllegalFileAccessException.class)
  public void testCreateRequestIllegalFile() throws Exception {
    final URI uri = logbackXml.toURI();
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

    request.execute();
  }

  @Test
  public void testCreateRequestDataGet() throws Exception {
    final URI uri =
        new URI(
            "data:image/png;base64,iVBORw0KGgoAAA"
                + "ANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"
                + "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU"
                + "5ErkJggg==");
    final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);
    final ClientHttpResponse response = request.execute();
    assertEquals(HttpStatus.OK, response.getStatusCode());

    final byte[] actual = IOUtils.toByteArray(response.getBody());
    assertEquals(85, actual.length);
  }
}
