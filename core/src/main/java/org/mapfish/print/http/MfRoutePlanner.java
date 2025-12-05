package org.mapfish.print.http;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.http.matcher.MatchInfo;
import org.springframework.http.HttpMethod;

/**
 * A Route planner that obtains proxies from the configuration that is currently in {@link
 * org.mapfish.print.http.MfClientHttpRequestFactoryImpl#CURRENT_CONFIGURATION}.
 *
 * <p>{@link MfClientHttpRequestFactoryImpl.Request} will set the correct configuration before the
 * request is executed so that correct proxies will be set.
 */
public final class MfRoutePlanner extends DefaultRoutePlanner {
  /** Constructor. */
  public MfRoutePlanner() {
    super(null);
  }

  @Override
  protected HttpHost determineProxy(HttpHost target, HttpContext context) throws HttpException {
    Configuration config = MfClientHttpRequestFactoryImpl.getCurrentConfiguration();
    if (config == null) {
      return null;
    }
    var coreCtx = HttpCoreContext.cast(context);
    if (coreCtx == null || coreCtx.getRequest() == null) {
      return null;
    }
    var request = coreCtx.getRequest();
    final URI uri;
    try {
      uri = request.getUri();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    HttpMethod method = HttpMethod.valueOf(request.getMethod());

    final List<HttpProxy> proxies = config.getProxies();
    for (HttpProxy proxy : proxies) {
      try {
        if (proxy.matches(MatchInfo.fromUri(uri, method))) {
          return proxy.getHttpHost();
        }
      } catch (SocketException | UnknownHostException | MalformedURLException e) {
        throw new HttpException(e.getMessage(), e);
      }
    }
    return null;
  }
}
