package org.mapfish.print.config;

import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Email sending configuration.
 *
 * <p>This configuration is needed only if reports are to be sent to the user by email.
 */
public class SmtpConfig implements ConfigurationObject {
  /** The default subject. */
  public static final String DEFAULT_SUBJECT = "MapFish print document";

  /** The default body. */
  public static final String DEFAULT_BODY = "Please find attached the requested document";

  /** The default body if there is a storage. */
  public static final String DEFAULT_BODY_STORAGE =
      "Please find the requested document there: {url}";

  /** The default subject in case of error. */
  public static final String DEFAULT_ERROR_SUBJECT = "MapFish print error";

  /** The default body in case of error. */
  public static final String DEFAULT_ERROR_BODY = "The print job failed:<br>{message}";

  private String fromAddress;
  private String host;
  private int port = 25;
  private String username;
  private String password;
  private boolean starttls = false;
  private boolean ssl = false;
  private String subject = DEFAULT_SUBJECT;
  private String body = null;
  private String errorSubject = DEFAULT_ERROR_SUBJECT;
  private String errorBody = DEFAULT_ERROR_BODY;
  private ReportStorage storage = null;

  @Override
  public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
    if (fromAddress == null) {
      validationErrors.add(new ConfigurationException("Missing fromAddress"));
    }

    if (host == null) {
      validationErrors.add(new ConfigurationException("Missing host"));
    }

    if (username == null ^ password == null) {
      validationErrors.add(new ConfigurationException("Missing username or password"));
    }

    if (ssl && starttls) {
      validationErrors.add(
          new ConfigurationException("Cannot enable ssl and starttls at the same time"));
    }

    if (storage != null) {
      storage.validate(validationErrors, configuration);
    }
  }

  public String getFromAddress() {
    return fromAddress;
  }

  /**
   * The email address used as "From:" in every email.
   *
   * @param fromAddress The address
   */
  public void setFromAddress(final String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getHost() {
    return host;
  }

  /**
   * The SMTP server hostname.
   *
   * @param host The host
   */
  public void setHost(final String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  /**
   * The TCP port of the SMTP server.
   *
   * @param port The port
   */
  public void setPort(final int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  /**
   * If auth is needed, the username.
   *
   * @param username The username
   */
  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  /**
   * If auth is needed, the password.
   *
   * @param password The password
   */
  public void setPassword(final String password) {
    this.password = password;
  }

  public boolean isStarttls() {
    return starttls;
  }

  /**
   * True to use STARTTLS.
   *
   * @param starttls Whatever
   */
  public void setStarttls(final boolean starttls) {
    this.starttls = starttls;
  }

  public boolean isSsl() {
    return ssl;
  }

  /**
   * True for enabling SSL. <br>
   * Cannot be enabled at the same time as <code>starttls</code>
   *
   * @param ssl The value
   */
  public void setSsl(final boolean ssl) {
    this.ssl = ssl;
  }

  @Nonnull
  public String getSubject() {
    return subject;
  }

  /**
   * The default email subject. <br>
   * This can be changed by the <code>smtp</code>.<code>subject</code> property in the request.
   *
   * @param subject The subject
   */
  public void setSubject(@Nonnull final String subject) {
    this.subject = subject;
  }

  /** Returns the configured body or the default value. */
  @Nonnull
  public String getBody() {
    if (body == null) {
      return storage == null ? DEFAULT_BODY : DEFAULT_BODY_STORAGE;
    } else {
      return body;
    }
  }

  /**
   * The default email body. <br>
   * This can be changed by the <code>smtp</code>.<code>body</code> property in the request.
   *
   * <p>If you have setup a storage, you must put a "{url}" marker where the URL to fetch the report
   * should be put.
   *
   * @param body The body
   */
  public void setBody(final String body) {
    this.body = body;
  }

  @Nullable
  public ReportStorage getStorage() {
    return storage;
  }

  /**
   * The report storage facility to use. <br>
   * By default, attaches the report in an email. But, for big files, this is not practical. This
   * can be used to configure a storage.
   *
   * @param storage The storage to use
   */
  public void setStorage(final ReportStorage storage) {
    this.storage = storage;
  }

  @Nonnull
  public String getErrorSubject() {
    return errorSubject;
  }

  /**
   * The default email subject in case of error. <br>
   * This can be changed by the <code>smtp</code>.<code>errorSubject</code> property in the request.
   *
   * @param errorSubject The subject
   */
  public void setErrorSubject(@Nonnull final String errorSubject) {
    this.errorSubject = errorSubject;
  }

  @Nonnull
  public String getErrorBody() {
    return errorBody;
  }

  /**
   * The default email body in case of error. <br>
   * This can be changed by the <code>smtp</code>.<code>body</code> property in the request.
   *
   * <p>The error message can be places in the text using a "{message}" marker.
   *
   * @param errorBody The body
   */
  public void setErrorBody(@Nonnull final String errorBody) {
    this.errorBody = errorBody;
  }
}
