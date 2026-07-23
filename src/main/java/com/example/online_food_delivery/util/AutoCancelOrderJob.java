package com.example.online_food_delivery.util;

import com.example.online_food_delivery.model.Order;
import com.example.online_food_delivery.model.OrderStatus;
import com.example.online_food_delivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_food_delivery.model.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelOrderJob {

    private final OrderRepository orderRepository;

    // Run every 3 seconds to auto-accept PLACED orders after 5 seconds
    @Scheduled(fixedRate = 3000)
    @Transactional
    public void autoAcceptOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(5);
        List<Order> placedOrders = orderRepository.findByStatus(OrderStatus.PLACED);

        for (Order order : placedOrders) {
            if (order.getPlacedAt().isBefore(threshold)) {
                order.setStatus(OrderStatus.ACCEPTED);
                orderRepository.save(order);
                log.info("Auto-accepted Order ID: {}", order.getId());
            }
        }
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelUnacceptedOrders() {
        log.info("Starting AutoCancelOrderJob...");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        List<Order> allOrders = orderRepository.findAll();
        
        List<Order> staleOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT
                        && o.getPlacedAt().isBefore(threshold))
                .toList();

        if (!staleOrders.isEmpty()) {
            log.info("Found {} stale orders to cancel", staleOrders.size());
            for (Order order : staleOrders) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("Cancelled Order ID: {}", order.getId());
            }
        }
    }

    // Run every 10 seconds to auto-complete refunds
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void processRefunds() {
        List<Order> refundingOrders = orderRepository.findByPaymentStatus(PaymentStatus.PROCESSING_REFUND);
        LocalDateTime now = LocalDateTime.now();
        for (Order order : refundingOrders) {
            if (order.getUpdatedAt() != null && order.getUpdatedAt().plusSeconds(10).isBefore(now)) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);
                log.info("Refund completed for Order ID: {} (₹{})", order.getId(), order.getRefundAmount());
            }
        }
    }
}
