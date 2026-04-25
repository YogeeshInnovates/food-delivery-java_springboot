package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.order_dto.OrderItemResponse;
import com.example.online_food_delivery.dto.order_dto.OrderResponse;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.*;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.OrderRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuthUtil authUtil;

    @Transactional
    public OrderResponse placeOrder() {
        User user = authUtil.currentUser();
        List<CartItem> cartItems = cartService.getCart();

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Group by restaurant (though simplified to one restaurant for now in real-world logic often)
        // Here we assume simple case: one restaurant per order
        Long restaurantId = cartItems.get(0).getRestaurantId();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .customer(user)
                .restaurant(restaurant)
                .status(OrderStatus.PLACED)
                .totalAmount(totalAmount)
                .items(new ArrayList<>())
                .build();

        for (CartItem cartItem : cartItems) {
            MenuItems menuItem = menuItemRepository.findById(cartItem.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(cartItem.getQuantity())
                    .priceAtOrder(cartItem.getPrice())
                    .build();
            order.getItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart();

        return mapToResponse(savedOrder);
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = authUtil.currentUser();
        return orderRepository.findByCustomer(user, pageable)
                .map(this::mapToResponse);
    }

    public OrderResponse getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Authorization check
        User user = authUtil.currentUser();
        if (!order.getCustomer().getId().equals(user.getId()) && 
            !order.getRestaurant().getOwner().getId().equals(user.getId()) &&
                !user.getRole().equals(Role.ADMIN)) {
            throw new UnauthorizedException("Unauthorized to view this order");
        }

        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        User user = authUtil.currentUser();
        // Validation: Only owner can update status for their restaurant
        if (!order.getRestaurant().getOwner().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ADMIN")) {
            throw new UnauthorizedException("Only restaurant owners can update order status");
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        User user = authUtil.currentUser();
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the customer who placed the order can cancel it");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new RuntimeException("Order can only be cancelled if it is in PLACED state");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countPendingOrders());
        stats.put("totalRevenue", orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getTotalAmount)
                .sum());
        return stats;
    }

    public Double getMyTotalSpend() {
        User user = authUtil.currentUser();
        return orderRepository.getTotalSpendByCustomer(user.getId());
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .restaurantName(order.getRestaurant().getName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .placedAt(order.getPlacedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .menuItemId(item.getMenuItem().getId())
                                .name(item.getMenuItem().getName())
                                .quantity(item.getQuantity())
                                .priceAtOrder(item.getPriceAtOrder())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
