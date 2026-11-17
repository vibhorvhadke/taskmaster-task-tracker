package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.auth.AuthResponse;
import com.taskmaster.taskmaster.dto.auth.LoginRequest;
import com.taskmaster.taskmaster.dto.auth.RegisterRequest;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * POST /api/auth/register -> 201 Created + token   (public)
 * POST /api/auth/login    -> 200 OK + token        (public)
 * POST /api/auth/logout   -> 204 No Content        (requires token)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.noContent().build();
    }
}