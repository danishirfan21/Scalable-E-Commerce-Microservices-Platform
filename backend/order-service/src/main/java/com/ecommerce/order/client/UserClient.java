package com.ecommerce.order.client;

import com.ecommerce.order.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with User Service.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    /**
     * Retrieves user details by ID.
     *
     * @param id the user ID
     * @return the user details
     */
    @GetMapping("/api/users/{id}")
    UserResponse getUser(@PathVariable("id") Long id);
}
