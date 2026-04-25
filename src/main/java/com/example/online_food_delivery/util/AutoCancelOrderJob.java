package com.example.online_food_delivery.util;

import com.example.online_food_delivery.model.Order;
import com.example.online_food_delivery.model.OrderStatus;
import com.example.online_food_delivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelOrderJob {

    private final OrderRepository orderRepository;

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelUnacceptedOrders() {
        log.info("Starting AutoCancelOrderJob...");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Order> allOrders = orderRepository.findAll();
        
        List<Order> staleOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PLACED && o.getPlacedAt().isBefore(threshold))
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
}
