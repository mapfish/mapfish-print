package org.mapfish.print.http;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.config.HasConfiguration;

/**
 * A configuration object for configuring a custom certificate/trust store.
 *
 * <p>It is a uri to a java jks keystore file along with the password for unlocking the store.
 */
public final class CertificateStore implements ConfigurationObject, HasConfiguration {
  private URI uri;
  private char[] password;
  private Configuration configuration;
  private volatile SSLContext sslContext;

  /** The uri to the certificate store. */
  public URI getUri() {
    return this.uri;
  }

  /**
   * The uri to the certificate store. It is a uri to a java jks keystore file along with the
   * password for unlocking the store.
   *
   * @param uri the uri to use for loading the file
   */
  public void setUri(final URI uri) {
    this.uri = uri;
  }

  /**
   * The password for unlocking the certificate store.
   *
   * @param password the password for unlocking the certificate store.
   */
  public void setPassword(final String password) {
    this.password = password.toCharArray();
  }

  @Override
  public void validate(final List<Throwable> validationErrors, final Configuration config) {
    if (this.uri == null) {
      validationErrors.add(new IllegalStateException("path is a required parameter"));
    }
  }

  /** Lazily create and get the ssl context. */
  public SSLContext getSSLContext() {

    if (this.sslContext == null) {
      synchronized (this) {
        if (this.sslContext == null) {
          this.sslContext = createSslContext();
        }
      }
    }
    return this.sslContext;
  }

  private SSLContext createSslContext() {
    try {
      String sslProtocol = System.getenv("PRINT_SSL_PROTOCOL");
      if (sslProtocol == null) {
        sslProtocol = "TLSv1.2";
      }
      SSLContext newSslContext = SSLContext.getInstance(sslProtocol);

      KeyStore ks = KeyStore.getInstance("JKS");

      final byte[] bytes = this.configuration.loadFile(this.uri.toString());
      ks.load(new ByteArrayInputStream(bytes), this.password);

      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, this.password);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);

      newSslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

      return newSslContext;
    } catch (Throwable t) {
      throw ExceptionUtils.getRuntimeException(t);
    }
  }

  @Override
  public void setConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }
}
