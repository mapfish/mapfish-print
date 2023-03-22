package org.mapfish.print.http;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.processor.http.matcher.DnsHostMatcher;
import org.mapfish.print.processor.http.matcher.LocalHostMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    locations = {
      AbstractMapfishSpringTest.DEFAULT_SPRING_XML,
      "classpath:/org/mapfish/print/http/proxy/application-context-proxy-test.xml"
    })
public class HttpProxyTest {
  static final int HTTPS_PROXY_PORT = 23433;
  static final String LOCALHOST = "localhost";
  private static final int PROXY_PORT = 23434;
  private static final String MESSAGE_FROM_PROXY = "Message From Proxy";
  private static final int TARGET_PORT = 22434;
  private static HttpServer targetServer;
  private static HttpServer proxyServer;
  private static HttpsServer httpsServer;

  @Autowired ConfigurationFactory configurationFactory;
  @Autowired private MfClientHttpRequestFactoryImpl requestFactory;

  @BeforeClass
  public static void setUp() throws Exception {
    proxyServer = HttpServer.create(new InetSocketAddress(LOCALHOST, PROXY_PORT), 0);
    proxyServer.start();

    targetServer = HttpServer.create(new InetSocketAddress(LOCALHOST, TARGET_PORT), 0);
    targetServer.start();

    httpsServer = createHttpsServer(HTTPS_PROXY_PORT);
  }

  @AfterClass
  public static void tearDown() {
    proxyServer.stop(0);
    targetServer.stop(0);
    httpsServer.stop(0);
  }

  static void assertCorrectResponse(
      ConfigurationFactory configurationFactory,
      MfClientHttpRequestFactoryImpl requestFactory,
      HttpCredential httpCredential,
      String expected,
      String target,
      String path)
      throws Exception {
    final File configFile =
        AbstractMapfishSpringTest.getFile(HttpProxyTest.class, "proxy/config.yaml");
    configurationFactory.setDoValidation(false);
    final Configuration config = configurationFactory.getConfig(configFile);
    final CertificateStore certificateStore = new CertificateStore();
    certificateStore.setConfiguration(config);
    certificateStore.setPassword("password");
    certificateStore.setUri(getKeystoreFile().toURI());

    config.setCertificateStore(certificateStore);
    if (httpCredential instanceof HttpProxy) {
      config.setProxies(Collections.singletonList((HttpProxy) httpCredential));
    } else {
      config.setCredentials(Collections.singletonList(httpCredential));
    }

    ConfigFileResolvingHttpRequestFactory clientHttpRequestFactory =
        new ConfigFileResolvingHttpRequestFactory(requestFactory, config, "test");

    URI uri = new URI(target + path);
    final ClientHttpRequest request = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
    final ClientHttpResponse response = request.execute();

    final String message = IOUtils.toString(response.getBody(), Constants.DEFAULT_ENCODING);
    assertEquals(message, HttpStatus.OK, response.getStatusCode());

    assertEquals(expected, message);
  }

  private static SSLContext getSslContext()
      throws NoSuchAlgorithmException,
          KeyStoreException,
          IOException,
          CertificateException,
          UnrecoverableKeyException,
          KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    char[] password = "password".toCharArray();

    KeyStore ks = KeyStore.getInstance("JKS");
    final File keystoreFile = getKeystoreFile();
    FileInputStream fis = new FileInputStream(keystoreFile);
    ks.load(fis, password);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, password);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ks);

    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
  }

  private static File getKeystoreFile() {
    return AbstractMapfishSpringTest.getFile(HttpProxyTest.class, "proxy/keystore.jks");
  }

  public static void respond(HttpExchange httpExchange, String errorMessage, int responseCode)
      throws IOException {
    final byte[] bytes = errorMessage.getBytes(Constants.DEFAULT_CHARSET);
    httpExchange.sendResponseHeaders(responseCode, bytes.length);
    httpExchange.getResponseBody().write(bytes);
    httpExchange.close();
  }

  public static HttpsServer createHttpsServer(int httpsProxyPort) throws Exception {
    HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(httpsProxyPort), 0);
    SSLContext sslContext = getSslContext();

    final SSLEngine m_engine = sslContext.createSSLEngine();

    httpsServer.setHttpsConfigurator(
        new HttpsConfigurator(sslContext) {
          public void configure(HttpsParameters params) {
            params.setCipherSuites(m_engine.getEnabledCipherSuites());
            params.setProtocols(m_engine.getEnabledProtocols());
          }
        });
    httpsServer.start();

    return httpsServer;
  }

  @Test
  public void testValidate() {
    final HttpProxy httpProxy = new HttpProxy();

    List<Throwable> errors = new ArrayList<>();

    Configuration configuration = new Configuration();
    httpProxy.validate(errors, configuration);
    assertEquals(1, errors.size());

    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(PROXY_PORT);

    errors.clear();
    httpProxy.validate(errors, configuration);
    assertEquals(0, errors.size());
  }

  @Test
  public void testExecuteProxy() throws Exception {
    final HttpProxy httpProxy = new HttpProxy();
    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(PROXY_PORT);

    final String path = "/request";
    proxyServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            respond(httpExchange, MESSAGE_FROM_PROXY, 200);
          }
        });

    assertCorrectResponse(
        httpProxy, MESSAGE_FROM_PROXY, "http://" + LOCALHOST + ":" + TARGET_PORT, path);
  }

  @Test
  public void testExecuteProxyWithUsername() throws Exception {
    final HttpProxy httpProxy = new HttpProxy();
    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(HTTPS_PROXY_PORT);
    httpProxy.setUsername("username");

    final String path = "/username";
    httpsServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            final String authorization = httpExchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null) {
              httpExchange
                  .getResponseHeaders()
                  .add("WWW-Authenticate", "Basic realm=\"Test Site\"");
              httpExchange.sendResponseHeaders(401, 0);
              httpExchange.close();
            } else {
              final String expectedAuth = "Basic dXNlcm5hbWU6bnVsbA==";
              if (authorization.equals(expectedAuth)) {
                respond(httpExchange, MESSAGE_FROM_PROXY, 200);
              } else {
                final String errorMessage =
                    String.format(
                        "Expected authorization:\n" + "'%s' but got:\n" + "'%s'",
                        expectedAuth, authorization);
                respond(httpExchange, errorMessage, 500);
              }
            }
          }
        });

    assertCorrectResponse(
        httpProxy, MESSAGE_FROM_PROXY, "http://" + LOCALHOST + ":" + TARGET_PORT, path);
  }

  @Test
  public void testExecuteProxyWithUsernameAndPassword() throws Exception {
    final HttpProxy httpProxy = new HttpProxy();
    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(HTTPS_PROXY_PORT);
    httpProxy.setUsername("username");
    httpProxy.setPassword("password");

    final String path = "/usernameAndPassword";
    httpsServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            final String authorization = httpExchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null) {
              httpExchange
                  .getResponseHeaders()
                  .add("WWW-Authenticate", "Basic realm=\"Test Site\"");
              httpExchange.sendResponseHeaders(401, 0);
              httpExchange.close();
            } else {
              final String expectedAuth = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";
              if (authorization.equals(expectedAuth)) {
                respond(httpExchange, MESSAGE_FROM_PROXY, 200);
              } else {
                final String errorMessage =
                    "Expected authorization:\n'"
                        + expectedAuth
                        + "' but got:\n'"
                        + authorization
                        + "'";
                respond(httpExchange, errorMessage, 500);
              }
            }
          }
        });

    assertCorrectResponse(
        httpProxy, MESSAGE_FROM_PROXY, "http://" + LOCALHOST + ":" + TARGET_PORT, path);
  }

  @Test
  public void testExecuteProxyNoMatcher() throws Exception {
    final HttpProxy httpProxy = new HttpProxy();
    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(PROXY_PORT);
    final DnsHostMatcher dnsHostMatcher = new DnsHostMatcher();
    dnsHostMatcher.setHost("google.com");
    httpProxy.setMatchers(Collections.singletonList(dnsHostMatcher));
    final String message = "Target was reached without proxy";

    final String path = "/nomatch";
    targetServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            respond(httpExchange, message, 200);
          }
        });
    proxyServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            String msg = "Proxy was reached but in this test the proxy should not have been used.";
            respond(httpExchange, msg, 500);
          }
        });

    assertCorrectResponse(httpProxy, message, "http://" + LOCALHOST + ":" + TARGET_PORT, path);
  }

  @Test
  public void testExecuteProxyMatcherReject() throws Exception {
    final HttpProxy httpProxy = new HttpProxy();
    httpProxy.setHost(LOCALHOST);
    httpProxy.setPort(PROXY_PORT);
    final LocalHostMatcher localHostMatcher = new LocalHostMatcher();
    localHostMatcher.setReject(true);
    httpProxy.setMatchers(Collections.singletonList(localHostMatcher));
    final String message = "Target was reached without proxy";

    final String path = "/nomatch";
    targetServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            respond(httpExchange, message, 200);
          }
        });
    proxyServer.createContext(
        path,
        new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            String msg = "Proxy was reached but in this test the proxy should not have been used.";
            respond(httpExchange, msg, 500);
          }
        });

    assertCorrectResponse(httpProxy, message, "http://" + LOCALHOST + ":" + TARGET_PORT, path);
  }

  private void assertCorrectResponse(
      HttpProxy httpProxy, String expected, String target, String path) throws Exception {
    assertCorrectResponse(configurationFactory, requestFactory, httpProxy, expected, target, path);
  }
}
