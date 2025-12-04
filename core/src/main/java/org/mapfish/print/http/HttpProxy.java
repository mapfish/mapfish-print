package org.mapfish.print.http;

import java.util.List;
import org.apache.hc.core5.http.HttpHost;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.http.matcher.URIMatcher;

/**
 * This configuration object configures the proxies to be used by the system. This is configured as
 * one of the root elements of the config.yaml
 *
 * <p>Example - Proxy all requests except localhost and www.camptocamp.org:
 *
 * <pre><code>
 * proxies:
 *   - !proxy
 *     scheme: http
 *     host: proxy.host.com
 *     port: 8888
 *     username: username
 *     password: xyzpassword
 *     matchers:
 *       - !localMatch
 *         reject: true
 *       - !dnsMatch
 *         host: www.camptocamp.org
 *         reject: true
 *       - !acceptAll {}
 * </code></pre>
 */
public final class HttpProxy extends HttpCredential {
  private String host;
  private int port = 80;
  private String scheme;

  public HttpHost getHttpHost() {
    return new HttpHost(getScheme(), this.host, this.port);
  }

  private String getScheme() {
    if (this.scheme == null) {
      if (super.getUsername() == null) {
        return "http";
      } else {
        return "https";
      }
    }
    return this.scheme;
  }

  /**
   * The scheme (http, https) of the proxy.
   *
   * <p>This is optional, default is http if no username and https if there is a password
   *
   * @param scheme the scheme of the proxy
   */
  public void setScheme(final String scheme) {
    this.scheme = scheme;
  }

  /**
   * Matchers are used to choose which requests this proxy applies to.
   *
   * @param matchers the matchers to use to determine which requests the applies can be used for
   * @see org.mapfish.print.processor.http.matcher.URIMatcher
   * @see org.mapfish.print.processor.http.RestrictUrisProcessor
   */
  public void setMatchers(final List<? extends URIMatcher> matchers) {
    super.setMatchers(matchers);
  }

  /**
   * The host of the proxy. Can be a hostname or ip address.
   *
   * <p>This is required.
   *
   * @param host the host of the proxy
   */
  public void setHost(final String host) {
    this.host = host;
  }

  /**
   * The username for authenticating with the proxy.
   *
   * <p>This is optional
   *
   * @param username the username for authenticating with the proxy
   */
  public void setUsername(final String username) {
    super.setUsername(username);
  }

  /**
   * The password for authenticating with the proxy.
   *
   * <p>This is optional
   *
   * @param password the password for authenticating with the proxy
   */
  public void setPassword(final String password) {
    super.setPassword(password);
  }

  /**
   * The host of the proxy. Can be a hostname or ip address.
   *
   * <p>This is optional. The default value is 80.
   *
   * @param port the port of the proxy
   */
  public void setPort(final int port) {
    this.port = port;
  }

  @Override
  public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
    if (this.host == null) {
      validationErrors.add(new IllegalStateException("The parameter 'host' is required."));
    }
  }
}
