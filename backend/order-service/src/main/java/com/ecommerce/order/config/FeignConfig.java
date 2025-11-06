package com.ecommerce.order.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration for Feign clients.
 * Handles logging, error decoding, and request interceptors.
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Configures Feign logging level.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Custom error decoder for Feign clients.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Request interceptor to propagate headers across services.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                var request = attributes.getRequest();

                // Propagate user ID header
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // Propagate user role header
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }

                // Propagate authorization header if present
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }

                log.debug("Propagating headers to Feign client: X-User-Id={}, X-User-Role={}",
                        userId, userRole);
            }
        };
    }

    /**
     * Custom error decoder for handling Feign client errors.
     */
    @Slf4j
    static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            log.error("Feign client error - Method: {}, Status: {}, Reason: {}",
                    methodKey, response.status(), response.reason());

            return switch (response.status()) {
                case 404 -> new RuntimeException("Resource not found: " + methodKey);
                case 400 -> new RuntimeException("Bad request: " + methodKey);
                case 503 -> new RuntimeException("Service unavailable: " + methodKey);
                default -> defaultErrorDecoder.decode(methodKey, response);
            };
        }
    }
}
