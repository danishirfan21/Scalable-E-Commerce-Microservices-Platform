package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client for Product Service
 * Handles all product-related operations from Order Service
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * Get product details by ID
     * @param id Product ID
     * @return Product details
     */
    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    /**
     * Check if product has sufficient stock
     * @param id Product ID
     * @param quantity Quantity to check
     * @return true if stock is available, false otherwise
     */
    @GetMapping("/api/products/{id}/check-stock")
    Boolean checkStock(
        @PathVariable("id") Long id, 
        @RequestParam("quantity") Integer quantity
    );

    /**
     * Reduce product inventory (called when order is placed)
     * @param id Product ID
     * @param amount Amount to reduce
     */
    @PatchMapping("/api/products/{id}/reduce-inventory")
    void reduceInventory(
        @PathVariable("id") Long id, 
        @RequestParam("amount") Integer amount
    );

    /**
     * Restore product inventory (called when order is cancelled)
     * @param id Product ID
     * @param quantity Quantity to restore
     */
    @PutMapping("/api/products/{id}/restore-inventory")
    void restoreInventory(
        @PathVariable("id") Long id, 
        @RequestParam("quantity") Integer quantity
    );
}