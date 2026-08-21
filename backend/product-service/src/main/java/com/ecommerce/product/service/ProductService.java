package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;

import java.util.List;

/**
 * Service interface for Product operations
 */
public interface ProductService {

    /**
     * Create a new product
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Update an existing product
     */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /**
     * Get product by ID
     */
    ProductResponse getProductById(Long id);

    /**
     * Get all products
     */
    List<ProductResponse> getAllProducts();

    /**
     * Get products by category
     */
    List<ProductResponse> getProductsByCategory(String category);

    /**
     * Search products by name
     */
    List<ProductResponse> searchProductsByName(String searchTerm);

    /**
     * Delete product by ID
     */
    void deleteProduct(Long id);

    /**
     * Update product inventory/quantity
     */
    ProductResponse updateInventory(Long id, Integer quantity);

    /**
     * Reduce product inventory (for order processing). Uses an atomic conditional DB update so
     * concurrent requests cannot oversell the same stock.
     *
     * @return true if stock was available and reserved, false if insufficient stock
     */
    boolean reduceInventoryIfAvailable(Long id, Integer amount);

    /**
     * Reduce product inventory (for order processing). Throws InsufficientStockException if
     * stock is not available. Kept for direct/admin API usage; the async order flow uses
     * {@link #reduceInventoryIfAvailable} instead so it can react to failure without an exception.
     */
    ProductResponse reduceInventory(Long id, Integer amount);

    /**
     * Restore product inventory (order cancellation/rejection). Atomic increment.
     */
    ProductResponse restoreInventory(Long id, Integer amount);

    /**
     * Get product by SKU
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Check if product has sufficient stock
     */
    boolean checkStock(Long id, Integer requiredQuantity);

    /**
     * Get low stock products
     */
    List<ProductResponse> getLowStockProducts(Integer threshold);
}
