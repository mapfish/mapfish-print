package org.mapfish.print.config.access;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;

public class AccessAssertionTestUtil {
    public static void setCreds(String... role) {
        Collection<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (String roleName: role) {
            authorities.add(new SimpleGrantedAuthority(roleName));
        }
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
