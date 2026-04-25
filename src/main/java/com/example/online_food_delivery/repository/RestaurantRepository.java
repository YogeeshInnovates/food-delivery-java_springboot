package com.example.online_food_delivery.repository;

import com.example.online_food_delivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant,Long> {
Optional<Restaurant> findById(Long id);
List<Restaurant> findByCity(String city);
List<Restaurant> findByCuisine(String cusine);
List<Restaurant> findByCityAndCuisine(String city,String cuisine);
}
