package com.example.online_food_delivery.unittest;

import com.example.online_food_delivery.dto.order_dto.OrderResponse;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.*;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.OrderRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.service.CartService;
import com.example.online_food_delivery.service.OrderService;
import com.example.online_food_delivery.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Restaurant testRestaurant;
    private MenuItems testMenuItem;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();
        User owner = User.builder()
                .id(2L)
                .email("owner@example.com")
                .role(Role.OWNER)
                .build();
        testRestaurant = Restaurant.builder()
                .id(10L)
                .name("Testaurant")
                .owner(owner)
                .build();
        testMenuItem = MenuItems.builder()
                .id(100L)
                .name("Pizza")
                .price(9.99)
                .restaurant(testRestaurant)
                .build();
        testCartItem = CartItem.builder()
                .menuItemId(testMenuItem.getId())
                .restaurantId(testRestaurant.getId())
                .price(testMenuItem.getPrice())
                .quantity(2)
                .build();
        lenient().when(authUtil.currentUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("placeOrder should create order when cart is not empty")
    void testPlaceOrderSuccess() {
        // Arrange
        when(cartService.getCart()).thenReturn(Collections.singletonList(testCartItem));
        when(restaurantRepository.findById(testRestaurant.getId()))
                .thenReturn(Optional.of(testRestaurant));
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        // Capture order saved
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    o.setId(500L);
                    o.setPlacedAt(LocalDateTime.now());
                    return o;
                });

        // Act
        OrderResponse response = orderService.placeOrder();

        // Assert
        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(testRestaurant.getName(), response.getRestaurantName());
        assertEquals(2 * testMenuItem.getPrice(), response.getTotalAmount());
        verify(cartService).clearCart();
    }

    @Test
    @DisplayName("placeOrder should throw when cart is empty")
    void testPlaceOrderEmptyCart() {
        when(cartService.getCart()).thenReturn(Collections.emptyList());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.placeOrder());
        assertEquals("Cart is empty", ex.getMessage());
    }

    @Nested
    @DisplayName("Authorization checks for order details")
    class AuthorizationTests {
        private Order existingOrder;

        @BeforeEach
        void initOrder() {
            existingOrder = Order.builder()
                    .id(200L)
                    .customer(testUser)
                    .restaurant(testRestaurant)
                    .status(OrderStatus.PLACED)
                    .totalAmount(19.98)
                    .placedAt(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();
            when(orderRepository.findById(200L)).thenReturn(Optional.of(existingOrder));
        }

        @Test
        @DisplayName("getOrderDetails should succeed for owner")
        void testGetOrderDetailsOwner() {
            User owner = testRestaurant.getOwner();
            when(authUtil.currentUser()).thenReturn(owner);
            OrderResponse resp = orderService.getOrderDetails(200L);
            assertNotNull(resp);
            assertEquals(existingOrder.getId(), resp.getId());
        }

        @Test
        @DisplayName("getOrderDetails should fail for unauthorized user")
        void testGetOrderDetailsUnauthorized() {
            User other = User.builder().id(99L).email("other@example.com").role(Role.CUSTOMER).build();
            when(authUtil.currentUser()).thenReturn(other);
            assertThrows(UnauthorizedException.class, () -> orderService.getOrderDetails(200L));
        }
    }

    @Test
    @DisplayName("updateOrderStatus should allow restaurant owner")
    void testUpdateOrderStatusOwner() {
        Order order = Order.builder()
                .id(300L)
                .customer(testUser)
                .restaurant(testRestaurant)
                .status(OrderStatus.PLACED)
                .placedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findById(300L)).thenReturn(Optional.of(order));
        when(authUtil.currentUser()).thenReturn(testRestaurant.getOwner());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse resp = orderService.updateOrderStatus(300L, OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, resp.getStatus());
    }

    @Test
    @DisplayName("updateOrderStatus should reject non-owner")
    void testUpdateOrderStatusUnauthorized() {
        Order order = Order.builder()
                .id(301L)
                .restaurant(testRestaurant)
                .status(OrderStatus.PLACED)
                .build();
        when(orderRepository.findById(301L)).thenReturn(Optional.of(order));
        // currentUser is a normal customer
        when(authUtil.currentUser()).thenReturn(testUser);
        assertThrows(UnauthorizedException.class, () -> orderService.updateOrderStatus(301L, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("cancelOrder should succeed for ordering customer when status is PLACED")
    void testCancelOrderSuccess() {
        Order order = Order.builder()
                .id(400L)
                .customer(testUser)
                .restaurant(testRestaurant)
                .status(OrderStatus.PLACED)
                .placedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findById(400L)).thenReturn(Optional.of(order));
        orderService.cancelOrder(400L, "Changed my mind");
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder should fail for non‑customer")
    void testCancelOrderUnauthorized() {
        User other = User.builder().id(99L).email("other@example.com").role(Role.CUSTOMER).build();
        Order order = Order.builder()
                .id(401L)
                .customer(other)
                .status(OrderStatus.PLACED)
                .build();
        when(orderRepository.findById(401L)).thenReturn(Optional.of(order));
        when(authUtil.currentUser()).thenReturn(testUser);
        assertThrows(UnauthorizedException.class, () -> orderService.cancelOrder(401L, "reason"));
    }

    @Test
    @DisplayName("cancelOrder should reject when order not in PLACED state")
    void testCancelOrderWrongState() {
        Order order = Order.builder()
                .id(402L)
                .customer(testUser)
                .status(OrderStatus.DELIVERED)
                .build();
        when(orderRepository.findById(402L)).thenReturn(Optional.of(order));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.cancelOrder(402L, "reason"));
        assertEquals("Order can only be cancelled if it is in PENDING_PAYMENT, PLACED, or PREPARING state", ex.getMessage());
    }

    @Test
    @DisplayName("getPlatformStats aggregates repository data correctly")
    void testGetPlatformStats() {
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.countPendingOrders()).thenReturn(3L);
        // Mock delivered orders to calculate revenue
        Order delivered1 = Order.builder().status(OrderStatus.DELIVERED).totalAmount(20.0).build();
        Order delivered2 = Order.builder().status(OrderStatus.DELIVERED).totalAmount(30.0).build();
        when(orderRepository.findAll()).thenReturn(Arrays.asList(delivered1, delivered2));
        Map<String, Object> stats = orderService.getPlatformStats();
        assertEquals(10L, stats.get("totalOrders"));
        assertEquals(3L, stats.get("pendingOrders"));
        assertEquals(50.0, stats.get("totalRevenue"));
    }

    @Test
    @DisplayName("getMyTotalSpend uses repository correctly")
    void testGetMyTotalSpend() {
        when(orderRepository.getTotalSpendByCustomer(testUser.getId())).thenReturn(123.45);
        Double spend = orderService.getMyTotalSpend();
        assertEquals(123.45, spend);
    }
}
