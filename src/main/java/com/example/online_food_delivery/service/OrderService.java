package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.order_dto.OrderItemResponse;
import com.example.online_food_delivery.dto.order_dto.OrderResponse;
import com.example.online_food_delivery.dto.payment_dto.DeliveryAddressRequest;
import com.example.online_food_delivery.dto.payment_dto.PaymentRequest;
import com.example.online_food_delivery.exception.BadRequestException;
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
            throw new BadRequestException("Cart is empty");
        }

        // Group by restaurant (though simplified to one restaurant for now in
        // real-world logic often)
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
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
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

    @Transactional
    public OrderResponse payForOrder(Long orderId, PaymentRequest request) {
        User user = authUtil.currentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the customer who placed the order can pay");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order is not awaiting payment");
        }

        order.setPaymentMethod(request.getPaymentMethod());

        switch (request.getPaymentMethod()) {
            case COD:
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setStatus(OrderStatus.PLACED);
                break;
            case UPI:
            case CARD:
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setStatus(OrderStatus.PLACED);
                break;
            default:
                throw new BadRequestException("Invalid payment method");
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateDeliveryAddress(Long orderId, DeliveryAddressRequest request) {
        User user = authUtil.currentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the customer can update delivery address");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Can only update address before payment");
        }

        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());

        return mapToResponse(orderRepository.save(order));
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = authUtil.currentUser();
        return orderRepository.findByCustomerOrderByPlacedAtDesc(user, pageable)
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
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = authUtil.currentUser();
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the customer who placed the order can cancel it");
        }

    if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PREPARING) {
        throw new BadRequestException("Order can only be cancelled if it is in PENDING_PAYMENT, PLACED, or PREPARING state");
    }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            java.time.Duration elapsed = java.time.Duration.between(order.getPlacedAt(), java.time.LocalDateTime.now());
            long seconds = elapsed.getSeconds();
            double cutPercent;
            if (seconds <= 1) {
                cutPercent = 4.0;
            } else if (seconds <= 5) {
                cutPercent = 10.0;
            } else {
                cutPercent = 25.0;
            }
            double refund = order.getTotalAmount() * (1 - cutPercent / 100.0);
            order.setRefundAmount(Math.round(refund * 100.0) / 100.0);
            order.setPaymentStatus(PaymentStatus.PROCESSING_REFUND);
        }

        orderRepository.save(order);
        return mapToResponse(order);
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
        Double spend = orderRepository.getTotalSpendByCustomer(user.getId());
        return spend != null ? spend : 0.0;
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .restaurantName(order.getRestaurant().getName())
                .customerName(order.getCustomer().getName())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLatitude(order.getDeliveryLatitude())
                .deliveryLongitude(order.getDeliveryLongitude())
                .restaurantLatitude(order.getRestaurant().getLatitude())
                .restaurantLongitude(order.getRestaurant().getLongitude())
                .cancellationReason(order.getCancellationReason())
                .refundAmount(order.getRefundAmount())
                .placedAt(order.getPlacedAt().atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")))
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
