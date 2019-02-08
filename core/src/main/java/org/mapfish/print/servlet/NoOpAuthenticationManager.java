package org.mapfish.print.servlet;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * A no-op AuthenticationManager.
 */
public class NoOpAuthenticationManager implements AuthenticationManager {
    @Override
    public Authentication authenticate(
            final Authentication authentication) throws AuthenticationException {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.emptyList();
            }

            @Override
            public Object getCredentials() {
                return "";
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "";
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {

            }

            @Override
            public String getName() {
                return "anonymous";
            }
        };
    }

    public boolean isEraseCredentialsAfterAuthentication() {
        return true;
    }
}
