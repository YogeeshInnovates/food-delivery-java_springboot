package com.example.online_food_delivery.unittest;

import com.example.online_food_delivery.dto.cart_dto.CartItemRequest;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.model.CartItem;
import com.example.online_food_delivery.model.MenuItems;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.service.CartService;
import com.example.online_food_delivery.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CartService}.
 */
@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private MenuItems testMenuItem;
    private CartItemRequest cartItemRequest;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();
        Restaurant restaurant = Restaurant.builder()
                .id(10L)
                .name("Testaurant")
                .owner(testUser)
                .build();
        testMenuItem = MenuItems.builder()
                .id(100L)
                .name("Pizza")
                .price(9.99)
                .restaurant(restaurant)
                .build();
        cartItemRequest = CartItemRequest.builder()
                .menuItemId(testMenuItem.getId())
                .quantity(2)
                .build();
        lenient().when(authUtil.currentUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("addItemToCart stores a CartItem in Redis")
    void testAddItemToCart() {
        when(menuItemRepository.findById(testMenuItem.getId())).thenReturn(Optional.of(testMenuItem));
        cartService.addItemToCart(cartItemRequest);
        verify(hashOperations).put(eq("cart:" + testUser.getId()), eq(String.valueOf(testMenuItem.getId())),
                any(CartItem.class));
    }

    @Test
    @DisplayName("addItemToCart throws when menu item missing")
    void testAddItemToCartNotFound() {
        when(menuItemRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cartService.addItemToCart(cartItemRequest));
    }

    @Test
    @DisplayName("getCart returns mapped CartItem list")
    void testGetCart() {
        CartItem stored = CartItem.builder()
                .menuItemId(testMenuItem.getId())
                .name(testMenuItem.getName())
                .price(testMenuItem.getPrice().doubleValue())
                .quantity(2)
                .restaurantId(testMenuItem.getRestaurant().getId())
                .build();
        Map<Object, Object> fakeMap = new HashMap<>();
        fakeMap.put(String.valueOf(testMenuItem.getId()), stored);
        when(hashOperations.entries(eq("cart:" + testUser.getId()))).thenReturn(fakeMap);
        List<CartItem> result = cartService.getCart();
        assertEquals(1, result.size());
        assertEquals(stored.getName(), result.get(0).getName());
    }

    @Nested
    @DisplayName("updateItemQuantity")
    class UpdateItemQuantityTests {
        @Test
        @DisplayName("updates quantity when present")
        void testUpdateExists() {
            CartItem existing = CartItem.builder().menuItemId(testMenuItem.getId()).quantity(1).build();
            when(hashOperations.hasKey(eq("cart:" + testUser.getId()), eq(String.valueOf(testMenuItem.getId()))))
                    .thenReturn(true);
            when(hashOperations.get(eq("cart:" + testUser.getId()), eq(String.valueOf(testMenuItem.getId()))))
                    .thenReturn(existing);
            cartService.updateItemQuantity(testMenuItem.getId(), 5);
            assertEquals(5, existing.getQuantity());
            verify(hashOperations).put(eq("cart:" + testUser.getId()), eq(String.valueOf(testMenuItem.getId())),
                    eq(existing));
        }

        @Test
        @DisplayName("throws when item absent")
        void testUpdateMissing() {
            when(hashOperations.hasKey(eq("cart:" + testUser.getId()), anyString())).thenReturn(false);
            assertThrows(ResourceNotFoundException.class, () -> cartService.updateItemQuantity(999L, 3));
        }
    }

    @Test
    @DisplayName("removeItemFromCart deletes entry")
    void testRemoveItemFromCart() {
        cartService.removeItemFromCart(testMenuItem.getId());
        verify(hashOperations).delete(eq("cart:" + testUser.getId()), eq(String.valueOf(testMenuItem.getId())));
    }

    @Test
    @DisplayName("clearCart deletes whole key")
    void testClearCart() {
        cartService.clearCart();
        verify(redisTemplate).delete(eq("cart:" + testUser.getId()));
    }
}
