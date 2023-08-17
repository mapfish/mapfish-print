package org.mapfish.print.config.access;

import java.util.Collection;
import java.util.HashSet;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AccessAssertionTestUtil {
  private AccessAssertionTestUtil() {
    // should not be instantiated
  }

  public static void setCreds(final String... role) {
    Collection<SimpleGrantedAuthority> authorities = new HashSet<>();
    for (String roleName : role) {
      authorities.add(new SimpleGrantedAuthority(roleName));
    }
    Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
