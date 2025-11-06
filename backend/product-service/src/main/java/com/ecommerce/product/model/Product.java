package com.ecommerce.product.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
