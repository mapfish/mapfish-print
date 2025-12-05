package org.mapfish.print.http;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLSocket;

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;

/** A ssl socket factory that obtains the keystore from the current configuration. */
public final class MfTLSSocketStrategy implements TlsSocketStrategy {
  private final TlsSocketStrategy defaultStrategy = DefaultClientTlsStrategy.createDefault();

  @Override
  public SSLSocket upgrade(final Socket socket,
                           final String target,
                           final int port,
                           final Object attachment,
                           final HttpContext context) throws IOException {
    return getSocketStrategy().upgrade(socket, target, port, attachment, context);
  }

  private TlsSocketStrategy getSocketStrategy() {
    final Configuration config = MfClientHttpRequestFactoryImpl.getCurrentConfiguration();
    if (config == null || config.getCertificateStore() == null) {
      return this.defaultStrategy;
    }
    return new DefaultClientTlsStrategy(config.getCertificateStore().getSSLContext());
  }
}
