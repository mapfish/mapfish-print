package org.mapfish.print.processor.http;

import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.Processor;

/**
 * A flag interface indicating that this type of processor affects the {@link
 * org.mapfish.print.http.MfClientHttpRequestFactory} object.
 *
 * @param <PARAM> the type of parameter object required when creating the wrapper object.
 */
public interface HttpProcessor<PARAM> extends Processor<PARAM, Void> {

  /**
   * Create the {@link org.mapfish.print.http.MfClientHttpRequestFactory} to use.
   *
   * @param param extra parameters required to create the updated request factory
   * @param requestFactory the basic request factory. It should be unmodified and just wrapped with
   *     a proxy class.
   */
  MfClientHttpRequestFactory createFactoryWrapper(
      PARAM param, MfClientHttpRequestFactory requestFactory);
}
