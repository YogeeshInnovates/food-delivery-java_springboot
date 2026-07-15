package com.example.online_food_delivery.repository;

import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findById(Long id);

    List<Restaurant> findByOwnerId(Long ownerId);

    List<Restaurant> findByCityContainingIgnoreCase(String city);

    List<Restaurant> findByCuisineTypeContainingIgnoreCase(String cuisineType);

    List<Restaurant> findByCityContainingIgnoreCaseAndCuisineTypeContainingIgnoreCase(String city,
            String cuisineType);

    Page<Restaurant> findByStatusIn(List<RestaurantStatus> statuses, Pageable pageable);

    List<Restaurant> findByStatusIn(List<RestaurantStatus> statuses);

    List<Restaurant> findByStatusInAndCityContainingIgnoreCase(List<RestaurantStatus> statuses, String city);

    List<Restaurant> findByStatusInAndCuisineTypeContainingIgnoreCase(List<RestaurantStatus> statuses, String cuisineType);

    List<Restaurant> findByStatusInAndCityContainingIgnoreCaseAndCuisineTypeContainingIgnoreCase(
            List<RestaurantStatus> statuses, String city, String cuisineType);
}
