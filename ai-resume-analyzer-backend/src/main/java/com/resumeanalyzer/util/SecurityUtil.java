package com.resumeanalyzer.util;

import com.resumeanalyzer.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    // Returns the email of the currently logged-in user
    public static String getCurrentUserEmail() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // username = email in our app
        }

        throw new UnauthorizedException("Unable to extract user from token");
    }

    // Returns the role of the currently logged-in user
    public static String getCurrentUserRole() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElseThrow(() ->
                        new UnauthorizedException("No role found for user"));
    }

    // Checks if current user is admin
    public static boolean isAdmin() {
        return getCurrentUserRole().equals("ROLE_ADMIN");
    }
}