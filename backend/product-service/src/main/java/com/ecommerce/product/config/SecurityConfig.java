package com.ecommerce.product.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration for Product Service
 * Accepts X-User-Id and X-User-Roles headers from API Gateway
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/products",
                                "/api/products/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Filter to extract user authentication from API Gateway headers
     */
    @Slf4j
    private static class GatewayAuthenticationFilter extends OncePerRequestFilter {

        private static final String USER_ID_HEADER = "X-User-Id";
        private static final String USER_ROLES_HEADER = "X-User-Roles";

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String userId = request.getHeader(USER_ID_HEADER);
            String rolesHeader = request.getHeader(USER_ROLES_HEADER);

            if (userId != null && rolesHeader != null) {
                log.debug("Authenticating user from gateway headers: userId={}, roles={}", userId, rolesHeader);

                // Parse roles from comma-separated string
                List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .map(role -> {
                            // Ensure role has ROLE_ prefix for Spring Security
                            if (!role.startsWith("ROLE_")) {
                                return "ROLE_" + role;
                            }
                            return role;
                        })
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create authentication token
                GatewayAuthenticationToken authentication =
                        new GatewayAuthenticationToken(userId, authorities);
                authentication.setAuthenticated(true);

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User authenticated successfully: userId={}", userId);
            } else {
                log.debug("No authentication headers present in request");
            }

            filterChain.doFilter(request, response);
        }
    }

    /**
     * Custom authentication token for gateway-based authentication
     */
    private static class GatewayAuthenticationToken extends org.springframework.security.authentication.AbstractAuthenticationToken {

        private final String userId;

        public GatewayAuthenticationToken(String userId, List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.userId = userId;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return userId;
        }
    }
}
