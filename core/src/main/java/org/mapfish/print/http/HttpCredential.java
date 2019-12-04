package org.mapfish.print.http;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.processor.http.matcher.MatchInfo;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.mapfish.print.processor.http.matcher.UriMatchers;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents a set of credentials to use for an http request.  These can be configured in the Configuration
 * yaml file.
 *
 * <p>
 * <em>Note: proxies are also HttpCredentials and when searching for credentials, the proxies will also be
 * searched for credentials.</em>
 * </p>
 */
public class HttpCredential implements ConfigurationObject {
    private String username;
    private char[] password;
    private UriMatchers matchers = new UriMatchers();

    /**
     * Matchers are used to choose which requests the credentials apply to.
     *
     * @param matchers the matchers to use to determine which requests the credentials can be used
     *         for
     * @see org.mapfish.print.processor.http.matcher.URIMatcher
     * @see org.mapfish.print.processor.http.RestrictUrisProcessor
     */
    public void setMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers.setMatchers(matchers);
    }

    /**
     * Get the username.
     */
    protected String getUsername() {
        return this.username;
    }

    /**
     * The username for authenticating with the credentials.
     * <p>
     * This is optional
     * </p>
     *
     * @param username the username for authenticating with the credentials
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * The password for authenticating with the credentials.
     * <p>
     * This is optional
     * </p>
     *
     * @param password the password for authenticating with the credentials
     */
    public void setPassword(final String password) {
        if (password == null) {
            throw new IllegalArgumentException(
                    "Do not set a null password, simply exclude it from configuration file.  " +
                            "If there is supposed to be a password perhaps it has illegal characters, " +
                            "surround password with quotes");
        }
        this.password = password.toCharArray();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.username == null) {
            validationErrors.add(new IllegalStateException("The parameter 'username' is required."));
        }
    }

    /**
     * Returns true if this proxy should be used for the provided URI.
     *
     * @param matchInfo the information for making the patch
     */
    public boolean matches(final MatchInfo matchInfo)
            throws SocketException, UnknownHostException, MalformedURLException {
        return this.matchers.matches(matchInfo);
    }

    /**
     * Check if this applies to the provided authorization scope and return the credentials for that scope or
     * null if it doesn't apply to the scope.
     *
     * @param authscope the scope to test against.
     */
    @Nullable
    public final Credentials toCredentials(final AuthScope authscope) {
        try {

            if (!matches(MatchInfo.fromAuthScope(authscope))) {
                return null;
            }
        } catch (UnknownHostException | MalformedURLException | SocketException e) {
            throw new RuntimeException(e);
        }
        if (this.username == null) {
            return null;
        }

        final String passwordString;
        if (this.password != null) {
            passwordString = new String(this.password);
        } else {
            passwordString = null;
        }
        return new UsernamePasswordCredentials(this.username, passwordString);
    }

}
