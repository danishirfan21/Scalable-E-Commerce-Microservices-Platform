package com.ecommerce.order.repository;

import com.ecommerce.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderItem entity operations.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Finds all order items for a specific order.
     *
     * @param orderId the order ID
     * @return list of order items
     */
    // Explicit nested-path syntax (Order_Id, not OrderId): OrderItem.getOrderId() is a plain
    // convenience method, not a mapped attribute, and shadows the derived-query parser's usual
    // fallback of splitting "orderId" into the "order.id" association traversal.
    List<OrderItem> findByOrder_Id(Long orderId);

    /**
     * Finds all order items for a specific product.
     *
     * @param productId the product ID
     * @return list of order items containing the product
     */
    List<OrderItem> findByProductId(Long productId);
}
