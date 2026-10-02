package com.securebiz.shared.security;

import com.securebiz.identity.security.SecurityPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentActor {
    private CurrentActor() {}

    public static UUID userId() {
        return principal().userId();
    }

    public static UUID sessionId() {
        return principal().sessionId();
    }

    private static SecurityPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new IllegalStateException("Authenticated security principal is required");
        }
        return principal;
    }

    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
