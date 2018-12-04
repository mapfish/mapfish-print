package org.mapfish.print.config;

import java.util.List;

/**
 * Email sending configuration.
 * <p>
 * This configuration is needed only if reports are to be sent to the user by email.
 */
public class SmtpConfig implements ConfigurationObject {
    /**
     * The default subject.
     */
    public static final String DEFAULT_SUBJECT = "Mapfish print document";

    /**
     * The default body.
     */
    public static final String DEFAULT_BODY = "Please find attached the requested document";

    private String fromAddress;
    private String host;
    private int port = 25;
    private String username;
    private String password;
    private boolean starttls = false;
    private boolean ssl = false;
    private String subject = DEFAULT_SUBJECT;
    private String body = DEFAULT_BODY;

    @Override
    public void validate(
            final List<Throwable> validationErrors, final Configuration configuration) {
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
            validationErrors.add(new ConfigurationException(
                    "Cannot enable ssl and starttls at the same time"));
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
     * True for enabling SSL.
     * <br>
     * Cannot be enabled at the same time as <code>starttls</code>
     *
     * @param ssl The value
     */
    public void setSsl(final boolean ssl) {
        this.ssl = ssl;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * The default email subject.
     * <br>
     * This can be changed by the <code>smtp</code>.<code>subject</code>
     * property in the request.
     *
     * @param subject The subject
     */
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    /**
     * The default email body.
     * <br>
     * This can be changed by the <code>smtp</code>.<code>body</code> property
     * in the request.
     *
     * @param body The body
     */
    public void setBody(final String body) {
        this.body = body;
    }
}
