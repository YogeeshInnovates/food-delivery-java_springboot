package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.restaurantDto.CompleteRegistrationRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.exception.BadRequestException;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.MenuCategory;
import com.example.online_food_delivery.model.MenuItems;
import com.example.online_food_delivery.model.Menu_Available_status;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import com.example.online_food_delivery.model.Role;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.MenuItemRepository;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restRepo;
    private final MenuItemRepository menuItemRepository;
    private final AuthUtil authutil;

    public RestaurantResponse addRestaurent(RestaurantRequest data) {
        User user = authutil.currentUser();
        Restaurant res_data = Restaurant.builder()
                .name(data.getName())
                .cuisineType(data.getCuisineType())
                .description(data.getDescription())
                .address(data.getAddress())
                .city(data.getCity())
                .phoneNumber(data.getPhoneNumber())
                .openingTime(data.getOpeningTime())
                .closingTime(data.getClosingTime())
                .owner(user).build();

        Restaurant result = restRepo.save(res_data);
        return mapToResponse(result);
    }

    public Page<RestaurantResponse> listRestaurant(Pageable pageable) {
        Page<Restaurant> restaurants = restRepo.findByStatusIn(
                List.of(RestaurantStatus.OPEN), pageable);
        return restaurants.map(this::mapToResponse);
    }

    public RestaurantResponse getRestaurant(Long id) {
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("there is no restaurant found "));
        return mapToResponse(restaurant);
    }

    public RestaurantResponse getRestaurantPublic(Long id) {
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("there is no restaurant found "));
        if (restaurant.getStatus() != RestaurantStatus.OPEN) {
            throw new ResourceNotFoundException("Restaurant not available");
        }
        return mapToResponse(restaurant);
    }

    public List<RestaurantResponse> getMyRestaurants() {
        User user = authutil.currentUser();
        List<Restaurant> restaurants = restRepo.findByOwnerId(user.getId());
        return restaurants.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public RestaurantResponse getMyRestaurantById(Long id) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not the owner of this restaurant");
        }
        return mapToResponse(restaurant);
    }

    public List<RestaurantResponse> filterRestaurant(String city, String cuisine) {
        List<RestaurantStatus> visible = List.of(RestaurantStatus.OPEN);
        List<Restaurant> restaurants;
        if (city == null && cuisine == null) {
            restaurants = restRepo.findByStatusIn(visible);
        } else if (city != null && cuisine == null) {
            restaurants = restRepo.findByStatusInAndCityContainingIgnoreCase(visible, city);
        } else if (city == null && cuisine != null) {
            restaurants = restRepo.findByStatusInAndCuisineTypeContainingIgnoreCase(visible, cuisine);
        } else {
            restaurants = restRepo.findByStatusInAndCityContainingIgnoreCaseAndCuisineTypeContainingIgnoreCase(visible, city, cuisine);
        }

        return restaurants.stream().map(this::mapToResponse).toList();
    }

    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest req) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Restaurants found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException(("You are not allowed to update this content"));
        }
        restaurant.setName(req.getName());
        restaurant.setCuisineType(req.getCuisineType());
        restaurant.setAddress(req.getAddress());
        if (req.getCity() != null) restaurant.setCity(req.getCity());
        if (req.getDescription() != null) restaurant.setDescription(req.getDescription());
        if (req.getPhoneNumber() != null) restaurant.setPhoneNumber(req.getPhoneNumber());
        if (req.getOpeningTime() != null) restaurant.setOpeningTime(req.getOpeningTime());
        if (req.getClosingTime() != null) restaurant.setClosingTime(req.getClosingTime());
        Restaurant updated_data = restRepo.save(restaurant);
        return mapToResponse(updated_data);
    }

    public RestaurantResponse updateRestaurantImage(Long id, String imageUrl) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not allowed to update this restaurant's image");
        }
        restaurant.setImageUrl(imageUrl);
        Restaurant updated = restRepo.save(restaurant);
        return mapToResponse(updated);
    }

    public void removeRestaurantImage(Long id) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not allowed to update this restaurant's image");
        }
        restaurant.setImageUrl(null);
        restRepo.save(restaurant);
    }

    public RestaurantResponse update_Status(Long id) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You not allow to toggle this function");
        }

        if (restaurant.getStatus() == RestaurantStatus.PENDING) {
            throw new BadRequestException("This restaurant is not available for longer time");
        }
        if (restaurant.getStatus() == RestaurantStatus.OPEN || restaurant.getStatus() == RestaurantStatus.ACTIVE) {
            restaurant.setStatus(RestaurantStatus.CLOSED);
        } else {
            restaurant.setStatus(RestaurantStatus.OPEN);
        }
        Restaurant updated = restRepo.save(restaurant);
        return mapToResponse(updated);
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveRestaurant(Long id) {
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        restaurant.setStatus(RestaurantStatus.OPEN);
        restRepo.save(restaurant);
    }

    @Transactional
    public void toggleRestaurantApproval(Long id) {
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (restaurant.getStatus() == RestaurantStatus.OPEN) {
            restaurant.setStatus(RestaurantStatus.ACTIVE);
        } else {
            restaurant.setStatus(RestaurantStatus.OPEN);
        }
        restRepo.save(restaurant);
    }

    @Transactional
    public RestaurantResponse completeRegistration(Long id, CompleteRegistrationRequest request) {
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to complete registration for this restaurant");
        }

        if (request.getMenuItems() == null || request.getMenuItems().isEmpty()) {
            throw new BadRequestException("At least one menu item is required to complete registration");
        }

        restaurant.setName(request.getName());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setLicenseNumber(request.getLicenseNumber());
        if (request.getPhoneNumber() != null) restaurant.setPhoneNumber(request.getPhoneNumber());
        if (request.getOpeningTime() != null) restaurant.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null) restaurant.setClosingTime(request.getClosingTime());
        restaurant.setImageUrl(request.getRestaurantImageUrl());
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        Restaurant savedRestaurant = restRepo.save(restaurant);

        for (CompleteRegistrationRequest.MenuItemEntry entry : request.getMenuItems()) {
            MenuCategory category = MenuCategory.MAIN_COURSE;
            if (entry.getCategory() != null && !entry.getCategory().isBlank()) {
                try {
                    category = MenuCategory.valueOf(entry.getCategory().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException ignored) {}
            }

            MenuItems menuItem = MenuItems.builder()
                    .name(entry.getName())
                    .price(entry.getPrice())
                    .category(category)
                    .description(entry.getDescription())
                    .restaurant(savedRestaurant)
                    .isVeg(entry.getIsVeg() != null ? entry.getIsVeg() : false)
                    .isAvailable(entry.getIsAvailable() != null ? entry.getIsAvailable() : true)
                    .status(Menu_Available_status.AVAILABLE)
                    .imageUrl(entry.getImageUrl())
                    .isDeleted(false)
                    .build();

            menuItemRepository.save(menuItem);
        }

        return mapToResponse(savedRestaurant);
    }

    private RestaurantResponse mapToResponse(Restaurant r) {
        String ownerName = r.getOwner() != null ? r.getOwner().getName() : "System";
        String ownerEmail = r.getOwner() != null ? r.getOwner().getEmail() : null;
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .cuisineType(r.getCuisineType())
                .description(r.getDescription())
                .address(r.getAddress())
                .city(r.getCity())
                .phoneNumber(r.getPhoneNumber())
                .licenseNumber(r.getLicenseNumber())
                .openingTime(r.getOpeningTime())
                .closingTime(r.getClosingTime())
                .imageUrl(r.getImageUrl())
                .rating(r.getRating())
                .isActive(r.getIsActive())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .ownerName(ownerName)
                .ownerEmail(ownerEmail)
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .build();
    }
}
