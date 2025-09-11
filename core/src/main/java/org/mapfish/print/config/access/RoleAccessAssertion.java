package org.mapfish.print.config.access;

import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.annotation.Nullable;
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

/** An access assertion that verifies that the current user has the required roles. */
public final class RoleAccessAssertion implements AccessAssertion {

  private static final String JSON_ROLES = "roles";
  private Set<String> requiredRoles;

  /**
   * Set the roles required to allow access. If not roles then the user must be logged in but does
   * not have to have any particular role.
   *
   * <p>This method may only be called once, any subsequent calls will result in errors.
   *
   * @param assertionRequiredRoles the roles required to access the protected resource
   */
  @SuppressWarnings("unchecked")
  public AccessAssertion setRequiredRoles(final Collection<String> assertionRequiredRoles) {
    if (this.requiredRoles != null) {
      throw new AssertionError(
          getClass()
              + "#setRequiredRoles() may only be called once any further calls"
              + " result in an exception");
    }
    if (assertionRequiredRoles == null) {
      this.requiredRoles = Collections.unmodifiableSet(Collections.emptySet());
    } else {
      if (assertionRequiredRoles instanceof Set) {
        Set roles = (Set) assertionRequiredRoles;
        this.requiredRoles = Collections.unmodifiableSet(roles);
      } else {
        this.requiredRoles = Collections.unmodifiableSet(new HashSet<>(assertionRequiredRoles));
      }
    }
    return this;
  }

  @Override
  public void assertAccess(final String resourceDescription, final Object protectedResource) {
    final SecurityContext context = SecurityContextHolder.getContext();

    if (context == null || context.getAuthentication() == null) {
      throw new AuthenticationCredentialsNotFoundException(
          resourceDescription + " requires an authenticated user");
    } else if (this.requiredRoles.isEmpty()) {
      if (!context.getAuthentication().getAuthorities().isEmpty()) {
        return;
      }
    } else {
      Collection<String> authorities =
          Collections2.transform(
              context.getAuthentication().getAuthorities(),
              (@Nullable final GrantedAuthority input) -> (input == null ? "" : input.toString()));
      for (String acc : this.requiredRoles) {
        if (authorities.contains(acc)) {
          return;
        }
      }
    }
    throw new AccessDeniedException(
        "User "
            + context.getAuthentication().getPrincipal()
            + " does not have one of the required roles to access: "
            + resourceDescription);
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
      this.requiredRoles = new HashSet<>();
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

  @Override
  public boolean equals(final Object o) {
    if (o instanceof RoleAccessAssertion) {
      return ((RoleAccessAssertion) o).requiredRoles.equals(this.requiredRoles);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.requiredRoles.hashCode();
  }

  @Override
  public AccessAssertion copy() {
    RoleAccessAssertion assertion = new RoleAccessAssertion();
    assertion.requiredRoles = Collections.unmodifiableSet(new HashSet<>(this.requiredRoles));
    return assertion;
  }
}
