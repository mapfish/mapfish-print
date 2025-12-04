package org.mapfish.print.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.mapfish.print.config.Configuration;
import org.springframework.http.client.ClientHttpRequest;

/**
 * A request object that provides low-level access so that the request can be configured for
 * proxying, authentication, etc...
 */
public interface ConfigurableRequest extends ClientHttpRequest {
  /** Obtain the request object. */
  HttpUriRequestBase getUnderlyingRequest();

  /**
   * Set the current configuration object. This should only be called by {@link
   * org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory}.
   *
   * @param configuration the config object for the current print job.
   */
  void setConfiguration(Configuration configuration);
}
