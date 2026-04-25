package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.cart_dto.CartItemRequest;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.model.CartItem;
import com.example.online_food_delivery.model.MenuItems;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MenuItemRepository menuItemRepository;
    private final AuthUtil authUtil;

    private static final String CART_PREFIX = "cart:";

    public void addItemToCart(CartItemRequest request) {
        User user = authUtil.currentUser();
        String cartKey = CART_PREFIX + user.getId();

        MenuItems menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        CartItem cartItem = CartItem.builder()
                .menuItemId(menuItem.getId())
                .name(menuItem.getName())
                .price(menuItem.getPrice())
                .quantity(request.getQuantity())
                .restaurantId(menuItem.getRestaurant().getId())
                .build();

        redisTemplate.opsForHash().put(cartKey, String.valueOf(menuItem.getId()), cartItem);
    }

    public List<CartItem> getCart() {
        User user = authUtil.currentUser();
        String cartKey = CART_PREFIX + user.getId();
        Map<Object, Object> cartMap = redisTemplate.opsForHash().entries(cartKey);
        
        return cartMap.values().stream()
                .map(obj -> (CartItem) obj)
                .collect(Collectors.toList());
    }

    public void updateItemQuantity(Long itemId, Integer quantity) {
        User user = authUtil.currentUser();
        String cartKey = CART_PREFIX + user.getId();
        
        if (redisTemplate.opsForHash().hasKey(cartKey, String.valueOf(itemId))) {
            CartItem item = (CartItem) redisTemplate.opsForHash().get(cartKey, String.valueOf(itemId));
            if (item != null) {
                item.setQuantity(quantity);
                redisTemplate.opsForHash().put(cartKey, String.valueOf(itemId), item);
            }
        } else {
            throw new ResourceNotFoundException("Item not in cart");
        }
    }

    public void removeItemFromCart(Long itemId) {
        User user = authUtil.currentUser();
        String cartKey = CART_PREFIX + user.getId();
        redisTemplate.opsForHash().delete(cartKey, String.valueOf(itemId));
    }

    public void clearCart() {
        User user = authUtil.currentUser();
        String cartKey = CART_PREFIX + user.getId();
        redisTemplate.delete(cartKey);
    }
}
