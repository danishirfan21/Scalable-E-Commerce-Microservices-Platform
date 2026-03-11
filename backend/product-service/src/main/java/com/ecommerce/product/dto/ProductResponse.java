package com.ecommerce.product.dto;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for product responses
 */
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String category;
    private String imageUrl;
    private String sku;
    private boolean inStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String description, BigDecimal price, Integer quantity, String category, String imageUrl, String sku, boolean inStock, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.imageUrl = imageUrl;
        this.sku = sku;
        this.inStock = inStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Convert Product entity to ProductResponse DTO
     */
    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .sku(product.getSku())
                .inStock(product.isInStock())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static ProductResponseBuilder builder() {
        return new ProductResponseBuilder();
    }

    public static class ProductResponseBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        private String category;
        private String imageUrl;
        private String sku;
        private boolean inStock;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ProductResponseBuilder id(Long id) { this.id = id; return this; }
        public ProductResponseBuilder name(String name) { this.name = name; return this; }
        public ProductResponseBuilder description(String description) { this.description = description; return this; }
        public ProductResponseBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductResponseBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public ProductResponseBuilder category(String category) { this.category = category; return this; }
        public ProductResponseBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ProductResponseBuilder sku(String sku) { this.sku = sku; return this; }
        public ProductResponseBuilder inStock(boolean inStock) { this.inStock = inStock; return this; }
        public ProductResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProductResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProductResponse build() {
            return new ProductResponse(id, name, description, price, quantity, category, imageUrl, sku, inStock, createdAt, updatedAt);
        }
    }
}
