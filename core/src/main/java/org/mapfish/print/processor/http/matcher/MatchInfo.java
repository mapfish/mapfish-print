package org.mapfish.print.processor.http.matcher;

import java.net.MalformedURLException;
import java.net.URI;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.auth.AuthScope;
import org.springframework.http.HttpMethod;

/** Information required for performing a request match. */
public final class MatchInfo {
  /** A value representing all and any schemes. */
  public static final String ANY_SCHEME = null;

  /** A value representing all and any hosts. */
  public static final String ANY_HOST = null;

  /** A value representing all and any realms. */
  public static final String ANY_REALM = null;

  /** A value representing all and any paths. */
  public static final String ANY_PATH = null;

  /** A value representing all and any fragments. */
  public static final String ANY_FRAGMENT = null;

  /** A value representing all and any queries. */
  public static final String ANY_QUERY = null;

  /** A value representing all and any queries. */
  public static final HttpMethod ANY_METHOD = null;

  /** A value representing all and any ports. */
  public static final int ANY_PORT = -1;

  private final String scheme;
  private final String host;
  private final int port;
  private final String path;
  private final String fragment;
  private final String query;
  private final String realm;
  private final HttpMethod method;

  /**
   * Constructor.
   *
   * @param scheme the scheme to match.
   * @param host the host to match.
   * @param port the host to match.
   * @param realm the realm to match.
   */
  private MatchInfo(final String scheme, final String host, final int port, final String realm) {
    this.scheme = scheme;
    this.host = host;
    this.port = port;
    this.path = ANY_PATH;
    this.query = ANY_QUERY;
    this.fragment = ANY_FRAGMENT;
    this.realm = realm;
    this.method = ANY_METHOD;
  }

  /**
   * Constructor.
   *
   * @param uri the uri containing the parameters to match.
   * @param port the host to match.
   * @param method the method to match.
   */
  private MatchInfo(final URI uri, final int port, final HttpMethod method) {
    this.scheme = uri.getScheme();
    this.host = uri.getHost();
    this.port = port;
    this.path = uri.getPath();
    this.query = uri.getQuery();
    this.fragment = uri.getFragment();
    this.realm = ANY_REALM;
    this.method = method;
  }

  /**
   * Create an info object from a uri and the http method object.
   *
   * @param uri the uri
   * @param method the method
   */
  public static MatchInfo fromUri(final URI uri, final HttpMethod method) {
    int newPort = uri.getPort();
    if (newPort < 0) {
      try {
        newPort = uri.toURL().getDefaultPort();
      } catch (MalformedURLException | IllegalArgumentException e) {
        newPort = ANY_PORT;
      }
    }

    return new MatchInfo(uri, newPort, method);
  }

  /**
   * Create an info object from an authscope object.
   *
   * @param authscope the authscope
   */
  public static MatchInfo fromAuthScope(final AuthScope authscope) {

    String newScheme =
        Strings.CS.equals(authscope.getSchemeName(), ANY_SCHEME)
            ? ANY_SCHEME
            : authscope.getSchemeName();
    String newHost =
        Strings.CS.equals(authscope.getHost(), ANY_HOST) ? ANY_HOST : authscope.getHost();
    int newPort = authscope.getPort() == ANY_PORT ? ANY_PORT : authscope.getPort();
    String newRealm =
        Strings.CS.equals(authscope.getRealm(), ANY_REALM) ? ANY_REALM : authscope.getRealm();

    return new MatchInfo(newScheme, newHost, newPort, newRealm);
  }

  private static String valOrAny(final String val) {
    return val != null ? val : "*";
  }

  public String getScheme() {
    return this.scheme;
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getPath() {
    return this.path;
  }

  public String getFragment() {
    return this.fragment;
  }

  public String getQuery() {
    return this.query;
  }

  public String getRealm() {
    return this.realm;
  }

  /**
   * @return A string
   */
  @Override
  public String toString() {
    String result =
        String.format(
            "%s://%s:%s/%s",
            valOrAny(this.scheme),
            valOrAny(this.host),
            this.port != ANY_PORT ? Integer.toString(this.port) : "*",
            valOrAny(this.path));
    if (this.method != ANY_METHOD) {
      result = this.method + " " + result;
    }
    if (this.query != ANY_QUERY) {
      result += "?" + this.query;
    }
    if (this.fragment != ANY_FRAGMENT) {
      result += "#" + this.fragment;
    }
    return result;
  }
}
