package com.taskmaster.taskmaster.config;

import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.UnauthorizedException;
import com.taskmaster.taskmaster.repository.RevokedTokenRepository;
import com.taskmaster.taskmaster.security.JwtAuthenticationFilter;
import com.taskmaster.taskmaster.security.JwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import jakarta.servlet.DispatcherType;

/**
 * Security setup:
 *  - Stateless JWT authentication (no HTTP session, no cookies)
 *  - Public endpoints: register and login. Everything else requires a valid token.
 *  - 401 / 403 errors returned in the same ProblemDetail JSON format as all other errors
 *  - BCrypt password hashing
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login"
    };

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(JwtService jwtService,
                          RevokedTokenRepository revokedTokenRepository,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is for browser cookie sessions; a stateless API using
                // bearer tokens in the Authorization header does not need it.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // We provide our own POST /api/auth/logout that revokes the token.
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 401: no token, or token invalid / expired / revoked
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
                            String message = reason != null
                                    ? reason.toString()
                                    : "Authentication is required to access this resource";
                            handlerExceptionResolver.resolveException(
                                    request, response, null, new UnauthorizedException(message));
                        })
                        // 403: authenticated, but not allowed
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                handlerExceptionResolver.resolveException(
                                        request, response, null,
                                        new ForbiddenException("You do not have permission to access this resource")))
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService, revokedTokenRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /** BCrypt: slow by design, with a random salt per password. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Used by the login flow. Spring Security builds it from our
     * AppUserDetailsService and PasswordEncoder beans automatically.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}