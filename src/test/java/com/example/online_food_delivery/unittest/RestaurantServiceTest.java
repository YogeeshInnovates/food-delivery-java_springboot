package com.example.online_food_delivery.unittest;

import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.service.RestaurantService;
import com.example.online_food_delivery.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private RestaurantService restaurantService;

    private User owner;
    private RestaurantRequest request;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").build();
        request = new RestaurantRequest();
        request.setName("Pasta Place");
        request.setCity("Bangalore");
        request.setCuisineType("Italian");
    }

    @Test
    public void testAddRestaurant_success() {
        when(authUtil.currentUser()).thenReturn(owner);
        Restaurant saved = Restaurant.builder()
                .id(10L)
                .name(request.getName())
                .city(request.getCity())
                .cuisineType(request.getCuisineType())
                .owner(owner)
                .status(RestaurantStatus.PENDING)
                .build();
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(saved);
        RestaurantResponse resp = restaurantService.addRestaurent(request);
        assertEquals(saved.getId(), resp.getId());
        assertEquals(saved.getName(), resp.getName());
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    public void testListRestaurant_pagination() {
        Restaurant r1 = Restaurant.builder().id(1L).name("A").city("C1").cuisineType("Cui").rating(4.5)
                .status(RestaurantStatus.OPEN).owner(owner).build();
        Restaurant r2 = Restaurant.builder().id(2L).name("B").city("C2").cuisineType("Cui").rating(4.0)
                .status(RestaurantStatus.OPEN).owner(owner).build();
        Pageable pageable = PageRequest.of(0, 2);
        Page<Restaurant> page = new PageImpl<>(Arrays.asList(r1, r2), pageable, 2);
        when(restaurantRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(page);
        Page<RestaurantResponse> result = restaurantService.listRestaurant(pageable);
        assertEquals(2, result.getTotalElements());
        assertEquals("A", result.getContent().get(0).getName());
    }

    @Test
    public void testGetRestaurant_success() {
        Restaurant existing = Restaurant.builder().id(1L).name("A").city("C1").cuisineType("Cui").rating(4.5)
                .status(RestaurantStatus.OPEN).owner(owner).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existing));
        RestaurantResponse resp = restaurantService.getRestaurant(1L);
        assertEquals("A", resp.getName());
    }

    @Test
    public void testGetRestaurant_notFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getRestaurant(99L));
    }

    @Test
    public void testFilterRestaurant_noFilters() {
        Restaurant r = Restaurant.builder().id(1L).city("Bangalore").cuisineType("Italian").owner(owner).build();
        when(restaurantRepository.findByStatusIn(anyList())).thenReturn(Collections.singletonList(r));
        List<RestaurantResponse> list = restaurantService.filterRestaurant(null, null);
        assertEquals(1, list.size());
    }

    @Test
    public void testFilterRestaurant_byCity() {
        Restaurant r = Restaurant.builder().id(1L).city("Bangalore").cuisineType("Italian").owner(owner).build();
        when(restaurantRepository.findByStatusInAndCityContainingIgnoreCase(anyList(), eq("Bangalore"))).thenReturn(Collections.singletonList(r));
        List<RestaurantResponse> list = restaurantService.filterRestaurant("Bangalore", null);
        assertEquals(1, list.size());
        assertEquals("Bangalore", list.get(0).getCity());
    }

    @Test
    public void testFilterRestaurant_byCuisine() {
        Restaurant r = Restaurant.builder().id(1L).city("Bangalore").cuisineType("Italian").owner(owner).build();
        when(restaurantRepository.findByStatusInAndCuisineTypeContainingIgnoreCase(anyList(), eq("Italian"))).thenReturn(Collections.singletonList(r));
        List<RestaurantResponse> list = restaurantService.filterRestaurant(null, "Italian");
        assertEquals(1, list.size());
        assertEquals("Italian", list.get(0).getCuisineType());
    }

    @Test
    public void testFilterRestaurant_byCityAndCuisine() {
        Restaurant r = Restaurant.builder().id(1L).city("Bangalore").cuisineType("Italian").owner(owner).build();
        when(restaurantRepository.findByStatusInAndCityContainingIgnoreCaseAndCuisineTypeContainingIgnoreCase(anyList(), eq("Bangalore"), eq("Italian")))
                .thenReturn(Collections.singletonList(r));
        List<RestaurantResponse> list = restaurantService.filterRestaurant("Bangalore", "Italian");
        assertEquals(1, list.size());
    }

    @Test
    public void testUpdateRestaurant_ownerAllowed() {
        Restaurant existing = Restaurant.builder().id(5L).owner(owner).city("OldCity").name("Old").cuisineType("Old")
                .build();
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(authUtil.currentUser()).thenReturn(owner);
        RestaurantRequest updReq = new RestaurantRequest();
        updReq.setName("NewName");
        updReq.setCity("NewCity");
        updReq.setCuisineType("NewCuisine");
        Restaurant saved = Restaurant.builder().id(5L).owner(owner).name("NewName").city("NewCity")
                .cuisineType("NewCuisine").build();
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(saved);
        RestaurantResponse resp = restaurantService.updateRestaurant(5L, updReq);
        assertEquals("NewName", resp.getName());
        assertEquals("NewCity", resp.getCity());
    }

    @Test
    public void testUpdateRestaurant_unauthorized() {
        User other = User.builder().id(2L).build();
        Restaurant existing = Restaurant.builder().id(5L).owner(other).build();
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(authUtil.currentUser()).thenReturn(owner);
        RestaurantRequest updReq = new RestaurantRequest();
        assertThrows(UnauthorizedException.class, () -> restaurantService.updateRestaurant(5L, updReq));
    }

    @Test
    public void testUpdateRestaurant_notFound() {
        when(authUtil.currentUser()).thenReturn(owner);
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        RestaurantRequest updReq = new RestaurantRequest();
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.updateRestaurant(99L, updReq));
    }

    @Test
    public void testToggleStatus_openToClosed() {
        when(authUtil.currentUser()).thenReturn(owner);
        Restaurant existing = Restaurant.builder().id(7L).owner(owner).status(RestaurantStatus.OPEN).build();
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));
        RestaurantResponse resp = restaurantService.update_Status(7L);
        assertEquals(RestaurantStatus.CLOSED.name(), resp.getStatus());
    }

    @Test
    public void testToggleStatus_closedToOpen() {
        when(authUtil.currentUser()).thenReturn(owner);
        Restaurant existing = Restaurant.builder().id(7L).owner(owner).status(RestaurantStatus.CLOSED).build();
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));
        RestaurantResponse resp = restaurantService.update_Status(7L);
        assertEquals(RestaurantStatus.OPEN.name(), resp.getStatus());
    }

    @Test
    public void testToggleStatus_pendingThrowsException() {
        when(authUtil.currentUser()).thenReturn(owner);
        Restaurant existing = Restaurant.builder().id(7L).owner(owner).status(RestaurantStatus.PENDING).build();
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(existing));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> restaurantService.update_Status(7L));
        assertEquals("This restaurant is not available for longer time", ex.getMessage());
    }

    @Test
    public void testToggleStatus_unauthorized() {
        User other = User.builder().id(2L).build();
        Restaurant existing = Restaurant.builder().id(7L).owner(other).status(RestaurantStatus.OPEN).build();
        when(authUtil.currentUser()).thenReturn(owner);
        when(restaurantRepository.findById(7L)).thenReturn(Optional.of(existing));
        assertThrows(UnauthorizedException.class, () -> restaurantService.update_Status(7L));
    }

    @Test
    public void testToggleStatus_notFound() {
        when(authUtil.currentUser()).thenReturn(owner);
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> restaurantService.update_Status(99L));
    }

    @Test
    public void testGetAllRestaurants() {
        Restaurant r = Restaurant.builder().id(1L).city("Bangalore").cuisineType("Italian").owner(owner).build();
        when(restaurantRepository.findAll()).thenReturn(Collections.singletonList(r));
        List<RestaurantResponse> list = restaurantService.getAllRestaurants();
        assertEquals(1, list.size());
    }

    @Test
    public void testApproveRestaurant_success() {
        Restaurant existing = Restaurant.builder().id(8L).owner(owner).status(RestaurantStatus.PENDING).build();
        when(restaurantRepository.findById(8L)).thenReturn(Optional.of(existing));
        restaurantService.approveRestaurant(8L);
        assertEquals(RestaurantStatus.OPEN, existing.getStatus());
        verify(restaurantRepository).save(existing);
    }

    @Test
    public void testApproveRestaurant_notFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> restaurantService.approveRestaurant(99L));
        assertEquals("Restaurant not found", ex.getMessage());
    }

}
