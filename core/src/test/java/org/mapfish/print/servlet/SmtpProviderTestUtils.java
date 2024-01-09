package org.mapfish.print.servlet;

import de.saly.javamail.mock2.MockTransport;
import javax.mail.Provider;

/** A replacement provider that replaces the normal SMTP transport with a mock one. */
public class SmtpProviderTestUtils extends Provider {
  public SmtpProviderTestUtils() {
    super(Type.TRANSPORT, "smtp", MockTransport.class.getName(), "Camptocamp", null);
  }
}
