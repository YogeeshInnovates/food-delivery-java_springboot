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
    Page<Order> findByCustomer(User customer, Pageable pageable);
    List<Order> findByRestaurantIdOrderByPlacedAtDesc(Long restaurantId);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customer.id = :customerId AND o.status = 'DELIVERED'")
    Double getTotalSpendByCustomer(Long customerId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PLACED'")
    long countPendingOrders();
}
