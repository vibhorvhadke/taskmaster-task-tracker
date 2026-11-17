package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.auth.AuthResponse;
import com.taskmaster.taskmaster.dto.auth.LoginRequest;
import com.taskmaster.taskmaster.dto.auth.RegisterRequest;
import com.taskmaster.taskmaster.dto.user.UserResponse;
import com.taskmaster.taskmaster.entity.RevokedToken;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.exception.ConflictException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.repository.RevokedTokenRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registration, login and logout.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Creates a new account and returns a token, so the user is logged in immediately.
     *
     * @throws ConflictException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))   // store only the BCrypt hash
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    /**
     * Verifies email and password and returns a new token.
     * A wrong email or password throws BadCredentialsException,
     * which GlobalExceptionHandler turns into 401 "Invalid email or password".
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizeEmail(request.email()),
                        request.password()
                )
        );

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.id()));

        return buildAuthResponse(user);
    }

    /**
     * Revokes the token used for this request. Any later request with the same token gets 401.
     */
    @Transactional
    public void logout(AuthenticatedUser currentUser) {
        String tokenId = currentUser.tokenId();

        if (!revokedTokenRepository.existsByTokenId(tokenId)) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenId(tokenId)
                    .expiresAt(currentUser.tokenExpiresAt())
                    .build());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    /** Emails are stored in lowercase so "Priya@Example.com" and "priya@example.com" are the same account. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}