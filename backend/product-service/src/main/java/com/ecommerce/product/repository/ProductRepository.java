package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Product entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products by category
     */
    List<Product> findByCategory(String category);

    /**
     * Find products by name containing the search term (case-insensitive)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> findByNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Find product by SKU with quantity greater than specified amount
     */
    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.quantity > :minQuantity")
    Optional<Product> findBySkuAndQuantityGreaterThan(@Param("sku") String sku,
                                                       @Param("minQuantity") Integer minQuantity);

    /**
     * Find product by SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if product exists by SKU
     */
    boolean existsBySku(String sku);

    /**
     * Find all products with quantity less than specified threshold
     */
    @Query("SELECT p FROM Product p WHERE p.quantity < :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    /**
     * Find all products in stock (quantity > 0)
     */
    @Query("SELECT p FROM Product p WHERE p.quantity > 0")
    List<Product> findAllInStock();

    /**
     * Find products by category and in stock
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.quantity > 0")
    List<Product> findByCategoryAndInStock(@Param("category") String category);
}
