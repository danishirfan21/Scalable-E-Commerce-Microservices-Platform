package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders for a specific user.
     *
     * @param userId the user ID
     * @return list of orders for the user
     */
    List<Order> findByUserId(Long userId);

    /**
     * Finds all orders with a specific status.
     *
     * @param status the order status
     * @return list of orders with the given status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Finds all orders for a user with a specific status.
     *
     * @param userId the user ID
     * @param status the order status
     * @return list of orders matching the criteria
     */
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * Finds an order by ID with its order items fetched eagerly.
     *
     * @param id the order ID
     * @return Optional containing the order if found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * Finds all orders for a user with order items fetched eagerly.
     *
     * @param userId the user ID
     * @return list of orders with items
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);
}
