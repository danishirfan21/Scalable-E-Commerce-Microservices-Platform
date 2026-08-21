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
     * Request interceptor that propagates the caller's identity to downstream services
     * and additionally stamps every outgoing Feign call with an internal ROLE_ORDER_SERVICE
     * authority. Order-service needs to call product-service's inventory endpoints and
     * user-service's user lookup endpoint on behalf of the request, but those endpoints are
     * guarded by roles the end user (e.g. ROLE_CUSTOMER) does not hold. ROLE_ORDER_SERVICE is
     * a trusted internal-service identity that downstream services grant to order-service
     * specifically (see product-service/user-service SecurityConfig), analogous to a service
     * account. This mirrors the existing header-trust model (X-User-Id/X-User-Roles set by the
     * API Gateway after JWT validation) rather than re-validating a JWT at every hop.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String userId = null;
            String userRoles = "ROLE_ORDER_SERVICE";

            if (attributes != null) {
                var request = attributes.getRequest();
                userId = request.getHeader("X-User-Id");
                String callerRoles = request.getHeader("X-User-Roles");
                if (callerRoles != null && !callerRoles.isBlank()) {
                    userRoles = callerRoles + ",ROLE_ORDER_SERVICE";
                }
            }

            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }
            requestTemplate.header("X-User-Roles", userRoles);

            log.debug("Propagating headers to Feign client: X-User-Id={}, X-User-Roles={}", userId, userRoles);
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
