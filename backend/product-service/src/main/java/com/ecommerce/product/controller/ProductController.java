package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Product operations
 * Provides endpoints for product management
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Product Management", description = "APIs for managing products in the e-commerce catalog")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new product",
            description = "Creates a new product in the catalog. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "409", description = "Product with SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        log.info("Received request to create product with SKU: {}", request.getSku());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a product",
            description = "Updates an existing product. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("Received request to update product with ID: {}", id);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves a product by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("Received request to get product with ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Get all products",
            description = "Retrieves all products in the catalog"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.debug("Received request to get all products");
        List<ProductResponse> responses = productService.getAllProducts();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/category/{category}")
    @Operation(
            summary = "Get products by category",
            description = "Retrieves all products in a specific category"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category) {
        log.debug("Received request to get products by category: {}", category);
        List<ProductResponse> responses = productService.getProductsByCategory(category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search products by name",
            description = "Searches for products by name containing the search term"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Search term") @RequestParam String term) {
        log.debug("Received request to search products with term: {}", term);
        List<ProductResponse> responses = productService.searchProductsByName(term);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sku/{sku}")
    @Operation(
            summary = "Get product by SKU",
            description = "Retrieves a product by its SKU"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable String sku) {
        log.debug("Received request to get product with SKU: {}", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a product",
            description = "Deletes a product from the catalog. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.info("Received request to delete product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update product inventory",
            description = "Updates the inventory quantity for a product. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> updateInventory(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "New quantity") @RequestParam @Min(0) Integer quantity) {
        log.info("Received request to update inventory for product ID: {} to quantity: {}", id, quantity);
        ProductResponse response = productService.updateInventory(id, quantity);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reduce-inventory")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_SERVICE')")
    @Operation(
            summary = "Reduce product inventory",
            description = "Reduces the inventory quantity for a product. Used during order processing.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory reduced successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> reduceInventory(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Amount to reduce") @RequestParam @Min(1) Integer amount) {
        log.info("Received request to reduce inventory for product ID: {} by amount: {}", id, amount);
        ProductResponse response = productService.reduceInventory(id, amount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/check-stock")
    @Operation(
            summary = "Check product stock availability",
            description = "Checks if a product has sufficient stock"
    )
    @ApiResponse(responseCode = "200", description = "Stock check completed")
    public ResponseEntity<Boolean> checkStock(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Required quantity") @RequestParam @Min(1) Integer quantity) {
        log.debug("Received request to check stock for product ID: {} with quantity: {}", id, quantity);
        boolean hasStock = productService.checkStock(id, quantity);
        return ResponseEntity.ok(hasStock);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get low stock products",
            description = "Retrieves products with stock below the threshold. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Low stock products retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @Parameter(description = "Stock threshold") @RequestParam(defaultValue = "10") @Min(0) Integer threshold) {
        log.debug("Received request to get low stock products with threshold: {}", threshold);
        List<ProductResponse> responses = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(responses);
    }
}
