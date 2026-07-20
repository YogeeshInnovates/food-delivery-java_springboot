package com.example.online_food_delivery.unittest;

import com.example.online_food_delivery.dto.menu_dto.Menu_Request;
import com.example.online_food_delivery.dto.menu_dto.Menu_Response;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.*;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.service.MenuService;
import com.example.online_food_delivery.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private MenuService menuService;

    private User testUser;
    private Restaurant testRestaurant;
    private MenuItems testMenuItem;
    private Menu_Request addRequest;
    private Menu_Request updateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .role(Role.OWNER)
                .build();
        testRestaurant = Restaurant.builder()
                .id(10L)
                .name("Testaurant")
                .owner(testUser)
                .build();
        testMenuItem = MenuItems.builder()
                .id(100L)
                .name("Pizza")
                .price(9.99)
                .category(MenuCategory.PIZZA)
                .description("Delicious cheese pizza")
                .restaurant(testRestaurant)
                .status(Menu_Available_status.AVAILABLE)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        addRequest = Menu_Request.builder()
                .restaurantId(testRestaurant.getId())
                .name("Burger")
                .price(5.49)
                .category("BURGER")
                .description("Juicy beef burger")
                .build();
        updateRequest = Menu_Request.builder()
                .restaurantId(testRestaurant.getId())
                .name("Vegan Salad")
                .price(7.99)
                .category("SOUTH_INDIAN")
                .description("Fresh veg salad")
                .build();
        lenient().when(authUtil.currentUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("addMenuItem succeeds when owner adds a menu item")
    void testAddMenuItemSuccess() {
        when(restaurantRepository.findById(testRestaurant.getId()))
                .thenReturn(Optional.of(testRestaurant));
        when(menuItemRepository.save(any(MenuItems.class)))
                .thenAnswer(invocation -> {
                    MenuItems m = invocation.getArgument(0);
                    m.setId(200L);
                    m.setCreatedAt(LocalDateTime.now());
                    return m;
                });
        Menu_Response resp = menuService.addMenuItem(addRequest);
        assertNotNull(resp);
        assertEquals(200L, resp.getId());
        assertEquals(addRequest.getName(), resp.getName());
        assertEquals(testRestaurant.getId(), resp.getRestaurantId());
        verify(menuItemRepository).save(any(MenuItems.class));
    }

    @Test
    @DisplayName("addMenuItem throws UnauthorizedException when non-owner tries to add")
    void testAddMenuItemUnauthorized() {
        User other = User.builder().id(2L).email("other@example.com").role(Role.CUSTOMER).build();
        when(authUtil.currentUser()).thenReturn(other);
        when(restaurantRepository.findById(testRestaurant.getId()))
                .thenReturn(Optional.of(testRestaurant));
        assertThrows(UnauthorizedException.class, () -> menuService.addMenuItem(addRequest));
    }

    @Nested
    @DisplayName("getMenu pagination")
    class GetMenuTests {
        @Test
        @DisplayName("getMenu with category filters correctly")
        void testGetMenuWithCategory() {
            List<MenuItems> items = Arrays.asList(testMenuItem);
            when(menuItemRepository.findByRestaurantIdAndIsDeletedFalse(testRestaurant.getId()))
                    .thenReturn(items);
            Page<Menu_Response> page = menuService.getMenu(testRestaurant.getId(), "PIZZA",
                    PageRequest.of(0, 10));
            assertEquals(1, page.getTotalElements());
            assertEquals("Pizza", page.getContent().get(0).getName());
        }

        @Test
        @DisplayName("getMenu without category returns all items")
        void testGetMenuWithoutCategory() {
            List<MenuItems> items = Arrays.asList(testMenuItem);
            when(menuItemRepository.findByRestaurantIdAndIsDeletedFalse(testRestaurant.getId()))
                    .thenReturn(items);
            Page<Menu_Response> page = menuService.getMenu(testRestaurant.getId(), null,
                    PageRequest.of(0, 10));
            assertEquals(1, page.getTotalElements());
            assertEquals("Pizza", page.getContent().get(0).getName());
        }
    }

    @Test
    @DisplayName("updateMenuItem succeeds for owner")
    void testUpdateMenuItemSuccess() {
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        when(menuItemRepository.save(any(MenuItems.class)))
                .thenAnswer(i -> i.getArgument(0));
        Menu_Response resp = menuService.updateMenuItem(testMenuItem.getId(), updateRequest);
        assertEquals(updateRequest.getName(), resp.getName());
        assertEquals(updateRequest.getPrice(), resp.getPrice());
        assertEquals(updateRequest.getCategory(), resp.getCategory());
    }

    @Test
    @DisplayName("updateMenuItem throws UnauthorizedException for non-owner")
    void testUpdateMenuItemUnauthorized() {
        User other = User.builder().id(2L).email("other@example.com").role(Role.CUSTOMER).build();
        when(authUtil.currentUser()).thenReturn(other);
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        assertThrows(UnauthorizedException.class,
                () -> menuService.updateMenuItem(testMenuItem.getId(), updateRequest));
    }

    @Test
    @DisplayName("toggleAvailability flips status correctly")
    void testToggleAvailability() {
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        menuService.toggleAvailability(testMenuItem.getId());
        assertEquals(Menu_Available_status.UNAVAILABLE, testMenuItem.getStatus());
        verify(menuItemRepository).save(testMenuItem);
    }

    @Test
    @DisplayName("softDeleteMenuItem marks item as deleted")
    void testSoftDeleteMenuItem() {
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        menuService.softDeleteMenuItem(testMenuItem.getId());
        assertTrue(testMenuItem.isDeleted());
        verify(menuItemRepository).save(testMenuItem);
    }

    @Test
    @DisplayName("searchMenuItems returns matching items")
    void testSearchMenuItems() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuItems> repoPage = new PageImpl<>(Arrays.asList(testMenuItem), pageable, 1);
        when(menuItemRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("pizza", pageable))
                .thenReturn(repoPage);

        Page<Menu_Response> result = menuService.searchMenuItems("pizza", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Pizza", result.getContent().get(0).getName());
        verify(menuItemRepository).findByNameContainingIgnoreCaseAndIsDeletedFalse("pizza", pageable);
    }

    @Test
    @DisplayName("getPopularItems returns available items sorted by id desc")
    void testGetPopularItems() {
        Pageable pageable = PageRequest.of(0, 10);
        MenuItems item2 = MenuItems.builder()
                .id(101L)
                .name("Burger")
                .price(5.49)
                .category(MenuCategory.BURGER)
                .restaurant(testRestaurant)
                .isDeleted(false)
                .isAvailable(true)
                .status(Menu_Available_status.AVAILABLE)
                .build();
        List<RestaurantStatus> statuses = List.of(RestaurantStatus.ACTIVE, RestaurantStatus.OPEN);
        Page<MenuItems> repoPage = new PageImpl<>(Arrays.asList(item2, testMenuItem), pageable, 2);
        when(menuItemRepository.findPopularByRestaurantStatuses(eq(statuses), any(Pageable.class)))
                .thenReturn(repoPage);

        Page<Menu_Response> result = menuService.getPopularItems(pageable);

        assertEquals(2, result.getTotalElements());
        verify(menuItemRepository).findPopularByRestaurantStatuses(eq(statuses), any(Pageable.class));
    }

    @Test
    @DisplayName("updateMenuItemImage updates image URL for owner")
    void testUpdateMenuItemImage() {
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        when(menuItemRepository.save(any(MenuItems.class)))
                .thenAnswer(i -> i.getArgument(0));

        Menu_Response resp = menuService.updateMenuItemImage(testMenuItem.getId(), "https://example.com/img.jpg");

        assertEquals("https://example.com/img.jpg", resp.getImageUrl());
        assertEquals("https://example.com/img.jpg", testMenuItem.getImageUrl());
        verify(menuItemRepository).save(testMenuItem);
    }

    @Test
    @DisplayName("updateMenuItemImage throws UnauthorizedException for non-owner")
    void testUpdateMenuItemImageUnauthorized() {
        User other = User.builder().id(2L).email("other@example.com").role(Role.CUSTOMER).build();
        when(authUtil.currentUser()).thenReturn(other);
        when(menuItemRepository.findById(testMenuItem.getId()))
                .thenReturn(Optional.of(testMenuItem));
        assertThrows(UnauthorizedException.class,
                () -> menuService.updateMenuItemImage(testMenuItem.getId(), "https://example.com/img.jpg"));
    }

    @Test
    @DisplayName("updateMenuItemImage throws ResourceNotFoundException when item not found")
    void testUpdateMenuItemImageNotFound() {
        when(menuItemRepository.findById(999L))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> menuService.updateMenuItemImage(999L, "https://example.com/img.jpg"));
    }
}
