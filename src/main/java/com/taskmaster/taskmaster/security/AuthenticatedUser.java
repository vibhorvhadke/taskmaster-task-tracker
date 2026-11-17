package com.taskmaster.taskmaster.security;

import com.taskmaster.taskmaster.entity.User;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * The authenticated user for the current request.
 *
 * Created in two situations:
 *  - At login (forLogin): includes the password hash so Spring Security can verify the password.
 *  - From a JWT (fromToken): includes the token ID and expiry, needed for logout. No password.
 *
 * Controllers receive it with: @AuthenticationPrincipal AuthenticatedUser currentUser
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String password,
        String tokenId,
        Instant tokenExpiresAt
) implements UserDetails {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    /** Used by AppUserDetailsService during login. */
    public static AuthenticatedUser forLogin(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPassword(), null, null);
    }

    /** Used by JwtAuthenticationFilter for requests carrying a valid token. */
    public static AuthenticatedUser fromToken(Claims claims) {
        return new AuthenticatedUser(
                claims.get("userId", Long.class),
                claims.getSubject(),
                null,
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** Never print the password hash in logs. */
    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", email=" + email + "]";
    }
}