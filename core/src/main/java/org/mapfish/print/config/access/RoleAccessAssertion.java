/*
 *
 *  * Copyright (C) 2014  Camptocamp
 *  *
 *  * This file is part of MapFish Print
 *  *
 *  * MapFish Print is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MapFish Print is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.mapfish.print.config.access;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An access assertion that verifies that the current user has the required roles.
 *
 * @author Jesse on 10/7/2014.
 */
public final class RoleAccessAssertion implements AccessAssertion {

    private static final String JSON_ROLES = "roles";
    private Set<String> requiredRoles;

    /**
     * Set the roles required to allow access.  If not roles then the user must be logged in but does not have to have any particular
     * role.
     * <p>
     * This method may only be called once, any subsequent calls will result in errors.
     * </p>
     *
     * @param assertionRequiredRoles the roles required to access the protected resource
     */
    @SuppressWarnings("unchecked")
    public AccessAssertion setRequiredRoles(final Collection<String> assertionRequiredRoles) {
        if (this.requiredRoles != null) {
            throw new AssertionError(getClass() + "#setRequiredRoles() may only be called once any further calls result in an exception");
        }
        if (assertionRequiredRoles == null) {
            this.requiredRoles = Collections.unmodifiableSet(Collections.<String>emptySet());
        } else {
            if (assertionRequiredRoles instanceof Set) {
                Set roles = (Set) assertionRequiredRoles;
                this.requiredRoles = Collections.unmodifiableSet(roles);
            } else {
                this.requiredRoles = Collections.unmodifiableSet(Sets.newHashSet(assertionRequiredRoles));
            }
        }
        return this;
    }

    @Override
    public void assertAccess(final String resourceDescription, final Object protectedResource) {
        final SecurityContext context = SecurityContextHolder.getContext();

        if (context == null || context.getAuthentication() == null) {
            throw new AuthenticationCredentialsNotFoundException(resourceDescription + " requires an authenticated user");
        } else if (this.requiredRoles.isEmpty()) {
            if (!context.getAuthentication().getAuthorities().isEmpty()) {
                return;
            }
        } else {
            Collection<String> authorities = Collections2.transform(context.getAuthentication().getAuthorities(),
                    new Function<GrantedAuthority, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable final GrantedAuthority input) {
                            return input == null ? "" : input.toString();
                        }
                    });
            for (String acc : this.requiredRoles) {
                if (authorities.contains(acc)) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("User " + context.getAuthentication().getPrincipal() +
                                        " does not have one of the required roles to access: " +
                                        resourceDescription);
    }

    @Override
    public JSONObject marshal() {
        JSONObject marshalData = new JSONObject();
        JSONArray roles = new JSONArray();
        try {
            marshalData.put(JSON_ROLES, roles);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (this.requiredRoles != null) {
            for (String role : this.requiredRoles) {
                roles.put(role);
            }
        }
        return marshalData;
    }

    @Override
    public void unmarshal(final JSONObject encodedAssertion) {
        try {
            this.requiredRoles = Sets.newHashSet();
            final JSONArray roles = encodedAssertion.getJSONArray(JSON_ROLES);
            for (int i = 0; i < roles.length(); i++) {
                this.requiredRoles.add(roles.getString(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.requiredRoles == null) {
            validationErrors.add(new ConfigurationException("requiredRoles must be defined"));
        }
    }
}
