package com.taskmaster.taskmaster.security;

import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Loads a user from the database by email. Used by Spring Security during login
 * to compare the submitted password with the stored BCrypt hash.
 *
 * Defining this bean also stops Spring Boot from creating its default
 * in-memory user (the "Using generated security password" message).
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .map(AuthenticatedUser::forLogin)
                // Spring Security converts this into BadCredentialsException,
                // so the client cannot tell whether the email or the password was wrong.
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}