package com.ecommerce.product.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity representing a product in the e-commerce catalog
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_sku", columnList = "sku", unique = true),
        @Index(name = "idx_name", columnList = "name")
})
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(Long id, String name, String description, BigDecimal price, Integer quantity, String category, String imageUrl, String sku, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.imageUrl = imageUrl;
        this.sku = sku;
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        return quantity != null && quantity > 0;
    }

    /**
     * Check if sufficient quantity is available
     */
    public boolean hasSufficientStock(int requiredQuantity) {
        return quantity != null && quantity >= requiredQuantity;
    }

    /**
     * Reduce inventory quantity
     */
    public void reduceQuantity(int amount) {
        if (this.quantity >= amount) {
            this.quantity -= amount;
        } else {
            throw new IllegalArgumentException("Insufficient stock available");
        }
    }

    /**
     * Increase inventory quantity
     */
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        private String category;
        private String imageUrl;
        private String sku;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        ProductBuilder() {}

        public ProductBuilder id(Long id) { this.id = id; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public ProductBuilder category(String category) { this.category = category; return this; }
        public ProductBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ProductBuilder sku(String sku) { this.sku = sku; return this; }
        public ProductBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProductBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Product build() {
            return new Product(id, name, description, price, quantity, category, imageUrl, sku, createdAt, updatedAt);
        }
    }
}
