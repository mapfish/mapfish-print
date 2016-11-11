package org.mapfish.print.config.access;

import com.google.common.collect.Sets;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

public class AccessAssertionTestUtil {
    public static void setCreds(String... role) {
        Collection<SimpleGrantedAuthority> authorities = Sets.newHashSet();
        for (String roleName : role) {
            authorities.add(new SimpleGrantedAuthority(roleName));
        }
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
