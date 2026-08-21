package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Atomically decrements stock only if enough is available. The WHERE clause makes this a
     * single conditional UPDATE at the database level, so two concurrent requests for the last
     * unit of stock cannot both succeed: exactly one UPDATE affects a row (returns 1), the other
     * affects zero rows (returns 0), with no read-then-write race window. This is the mechanism
     * that prevents overselling under concurrent order creation.
     */
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :amount WHERE p.id = :id AND p.quantity >= :amount")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("amount") int amount);

    /**
     * Atomically increments stock (used to restore inventory on order cancellation/rejection).
     */
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :amount WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("amount") int amount);
}
