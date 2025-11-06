package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for communicating with Product Service.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * Retrieves product details by ID.
     *
     * @param id the product ID
     * @return the product details
     */
    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    /**
     * Checks if sufficient stock is available for a product.
     *
     * @param id the product ID
     * @param quantity the required quantity
     * @return true if stock is available, false otherwise
     */
    @GetMapping("/api/products/{id}/check-stock")
    Boolean checkStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    /**
     * Reduces inventory for a product (called when order is confirmed).
     *
     * @param id the product ID
     * @param quantity the quantity to reduce
     */
    @PutMapping("/api/products/{id}/reduce-inventory")
    void reduceInventory(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    /**
     * Restores inventory for a product (called when order is cancelled).
     *
     * @param id the product ID
     * @param quantity the quantity to restore
     */
    @PutMapping("/api/products/{id}/restore-inventory")
    void restoreInventory(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}
