package com.example.online_food_delivery.repository;

import com.example.online_food_delivery.model.Order;
import com.example.online_food_delivery.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByCustomerOrderByPlacedAtDesc(User customer, Pageable pageable);
    List<Order> findByRestaurantIdOrderByPlacedAtDesc(Long restaurantId);

    @Query("SELECT o FROM Order o WHERE o.restaurant.owner.id = :ownerId ORDER BY o.placedAt DESC")
    List<Order> findOrdersByOwnerId(Long ownerId);

    @Query("SELECT o FROM Order o WHERE o.restaurant.owner.id = :ownerId AND o.status = :status ORDER BY o.placedAt DESC")
    List<Order> findOrdersByOwnerIdAndStatus(Long ownerId, com.example.online_food_delivery.model.OrderStatus status);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customer.id = :customerId AND o.status NOT IN ('CANCELLED', 'PENDING_PAYMENT')")
    Double getTotalSpendByCustomer(Long customerId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PLACED'")
    long countPendingOrders();

    List<Order> findByPaymentStatus(com.example.online_food_delivery.model.PaymentStatus paymentStatus);

    List<Order> findByStatus(com.example.online_food_delivery.model.OrderStatus status);
}
