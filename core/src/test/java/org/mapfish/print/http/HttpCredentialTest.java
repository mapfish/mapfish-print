package org.mapfish.print.http;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpsServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.processor.http.matcher.DnsHostMatcher;
import org.mapfish.print.processor.http.matcher.MatchInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    locations = {
      AbstractMapfishSpringTest.DEFAULT_SPRING_XML,
      "classpath:/org/mapfish/print/http/proxy/application-context-proxy-test.xml"
    })
public class HttpCredentialTest {
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final int HTTPS_PROXY_PORT = 21433;
  private static HttpsServer httpsServer;

  @Autowired ConfigurationFactory configurationFactory;
  @Autowired private MfClientHttpRequestFactoryImpl requestFactory;

  @BeforeAll
  public static void setUp() throws Exception {
    httpsServer = HttpProxyTest.createHttpsServer(HTTPS_PROXY_PORT);
  }

  @AfterAll
  public static void tearDown() {
    httpsServer.stop(0);
  }

  @Test
  public void testValidate() {
    final HttpCredential credential = new HttpCredential();
    Configuration configuration = new Configuration();

    List<Throwable> errors = new ArrayList<>();
    credential.validate(errors, configuration);
    assertEquals(1, errors.size());

    errors.clear();
    credential.validate(errors, configuration);
    assertEquals(1, errors.size());

    credential.setUsername("username");

    errors.clear();
    credential.validate(errors, configuration);
    assertEquals(0, errors.size());
  }

  @Test
  public void testToCredentials() throws Exception {
    final HttpCredential credential = new HttpCredential();
    credential.setUsername(USERNAME);
    credential.setPassword(PASSWORD);

    final DnsHostMatcher matcher = new DnsHostMatcher();
    matcher.setHost(HttpProxyTest.LOCALHOST);
    credential.setMatchers(Collections.singletonList(matcher));

    AuthScope authscope = new AuthScope(null, -1);
    final UsernamePasswordCredentials object = credential.toCredentials(authscope);
    assertNotNull(object);
    assertEquals(USERNAME, object.getUserPrincipal().getName());
    assertEquals(PASSWORD, new String(object.getUserPassword()));

    authscope =
        new AuthScope(
            MatchInfo.ANY_SCHEME,
            MatchInfo.ANY_HOST,
            MatchInfo.ANY_PORT,
            MatchInfo.ANY_REALM,
            MatchInfo.ANY_SCHEME);
    assertNotNull(credential.toCredentials(authscope));

    authscope =
        new AuthScope(
            MatchInfo.ANY_SCHEME,
            MatchInfo.ANY_HOST,
            HttpProxyTest.HTTPS_PROXY_PORT,
            MatchInfo.ANY_REALM,
            MatchInfo.ANY_SCHEME);
    assertNotNull(credential.toCredentials(authscope));

    authscope =
        new AuthScope(
            MatchInfo.ANY_SCHEME,
            MatchInfo.ANY_HOST,
            80,
            MatchInfo.ANY_REALM,
            MatchInfo.ANY_SCHEME);
    assertNotNull(credential.toCredentials(authscope));

    authscope =
        new AuthScope(
            MatchInfo.ANY_SCHEME,
            "google.com",
            MatchInfo.ANY_PORT,
            MatchInfo.ANY_REALM,
            MatchInfo.ANY_SCHEME);
    assertNull(credential.toCredentials(authscope));

    authscope =
        new AuthScope(
            MatchInfo.ANY_SCHEME,
            MatchInfo.ANY_HOST,
            MatchInfo.ANY_PORT,
            MatchInfo.ANY_REALM,
            "http");
    assertNotNull(credential.toCredentials(authscope));
  }

  @Test
  public void testToHttpsBehaviour() throws Exception {
    final String message = "Message from server";

    final String path = "/username";
    httpsServer.createContext(
        path,
        httpExchange -> {
          final String authorization = httpExchange.getRequestHeaders().getFirst("Authorization");
          if (authorization == null) {
            httpExchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"Test Site\"");
            httpExchange.sendResponseHeaders(401, 0);
            httpExchange.close();
          } else {
            final String expectedAuth = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";
            if (authorization.equals(expectedAuth)) {
              HttpProxyTest.respond(httpExchange, message, 200);
            } else {
              final String errorMessage =
                  "Expected authorization:\n'"
                      + expectedAuth
                      + "' but got:\n'"
                      + authorization
                      + "'";
              HttpProxyTest.respond(httpExchange, errorMessage, 500);
            }
          }
        });

    final HttpCredential credential = new HttpCredential();
    credential.setUsername(USERNAME);
    credential.setPassword(PASSWORD);

    final DnsHostMatcher matcher = new DnsHostMatcher();
    matcher.setHost(HttpProxyTest.LOCALHOST);
    credential.setMatchers(Collections.singletonList(matcher));

    final String target = "https://" + HttpProxyTest.LOCALHOST + ":" + HTTPS_PROXY_PORT;
    HttpProxyTest.assertCorrectResponse(
        this.configurationFactory, this.requestFactory, credential, message, target, path);
  }
}
