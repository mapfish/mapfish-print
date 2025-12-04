package org.mapfish.print.http;

import java.util.ArrayList;
import java.util.List;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.mapfish.print.config.Configuration;

/**
 * A Route planner that obtains credentials from the configuration that is currently in {@link
 * org.mapfish.print.http.MfClientHttpRequestFactoryImpl#CURRENT_CONFIGURATION}.
 *
 * <p>If authentication is not found in configuration then it will fall back to {@link
 * org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider}
 *
 * <p>{@link MfClientHttpRequestFactoryImpl.Request} will set the correct configuration before the
 * request is executed so that correct proxies will be set.
 */
public final class MfCredentialsProvider implements CredentialsProvider {
  private final CredentialsProvider fallback = new SystemDefaultCredentialsProvider();

  @Override
  public void setCredentials(final AuthScope authscope, final Credentials credentials) {
    throw new UnsupportedOperationException(
        "Credentials should be set the default Java way or in the configuration yaml file.");
  }

  @Override
  public Credentials getCredentials(final AuthScope authscope) {

    Configuration config = MfClientHttpRequestFactoryImpl.getCurrentConfiguration();
    if (config != null) {
      List<HttpCredential> allCredentials = new ArrayList<>(config.getCredentials());
      allCredentials.addAll(config.getProxies());

      for (HttpCredential credential : allCredentials) {
        final Credentials credentials = credential.toCredentials(authscope);
        if (credentials != null) {
          return credentials;
        }
      }
    }
    return this.fallback.getCredentials(authscope);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(
        "Credentials should be set the default Java way or in the configuration yaml file.");
  }
}
