package com.example.online_food_delivery.service;

import com.example.online_food_delivery.dto.restaurantDto.RestaurantRequest;
import com.example.online_food_delivery.dto.restaurantDto.RestaurantResponse;
import com.example.online_food_delivery.exception.ResourceNotFoundException;
import com.example.online_food_delivery.exception.UnauthorizedException;
import com.example.online_food_delivery.model.Restaurant;
import com.example.online_food_delivery.model.RestaurantStatus;
import com.example.online_food_delivery.model.User;
import com.example.online_food_delivery.repository.RestaurantRepository;
import com.example.online_food_delivery.util.AuthUtil;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restRepo;
    private final AuthUtil authutil;

    public RestaurantResponse addRestaurent(RestaurantRequest data){
        User user = authutil.currentUser();
        Restaurant res_data = Restaurant.builder()
                        .name(data.getName())
                                .city(data.getCity())
                                        .cuisine(data.getCuisine())
                .owner(user).build();

        Restaurant result = restRepo.save(res_data);
        return new RestaurantResponse(result.getId(),result.getName(),result.getCity(),result.getRating(),result.getStatus().name(),result.getCuisine(),result.getCreatedAt(),result.getOwner().getName());
    }


    public Page<RestaurantResponse> listRestaurant(Pageable pageable){
        Page<Restaurant> restaurants = restRepo.findAll(pageable);
        return restaurants.map(r-> new RestaurantResponse(
                r.getId(),r.getName(),r.getCity(),r.getRating(),r.getStatus().name(),r.getCuisine(),r.getCreatedAt(), r.getOwner().getName()
                ));
    }

    public RestaurantResponse getRestaurant(Long id){
        Restaurant restaurant = restRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("there is no restaurant found "));
        return new RestaurantResponse(
                restaurant.getId(),restaurant.getName(),restaurant.getCity(),restaurant.getRating(),restaurant.getStatus().name(),restaurant.getCuisine(),restaurant.getCreatedAt(), restaurant.getOwner().getName()
        );

    }

    public List<RestaurantResponse> filterRestaurant(String city, String cuisine){
        List<Restaurant> restaurants;
     if(city ==null && cuisine == null){
        restaurants = restRepo.findAll();
     }
     else if(city !=null && cuisine == null){
         restaurants = restRepo.findByCity(city);
     }
     else if(city == null && cuisine != null){
         restaurants = restRepo.findByCuisine(cuisine);
     } else{
           restaurants = restRepo.findByCityAndCuisine(city,cuisine);
        }


        return restaurants.stream().map(r-> new RestaurantResponse( r.getId(),r.getName(),r.getCity(),r.getRating(),r.getStatus().name(),r.getCuisine(),r.getCreatedAt(), r.getOwner().getName()
        )).toList();
    }


    public RestaurantResponse updateRestaurant(Long id,RestaurantRequest req){
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("No Restaurants found"));
        if(!restaurant.getOwner().getId().equals(user.getId())){
           throw new UnauthorizedException(("You are not allow to update this content"));
        }
        restaurant.setCity(req.getCity());
      restaurant.setName(req.getName());
      restaurant.setCuisine(req.getCuisine());
      Restaurant updated_data = restRepo.save(restaurant);
        return new RestaurantResponse(updated_data.getId(),updated_data.getName(),updated_data.getCity(),updated_data.getRating(),updated_data.getStatus().name(),updated_data.getCuisine(),updated_data.getCreatedAt(),updated_data.getOwner().getName());
    }

    public RestaurantResponse update_Status(Long id){
        User user = authutil.currentUser();
        Restaurant restaurant = restRepo.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if(!restaurant.getOwner().getId().equals(user.getId())){
            throw new UnauthorizedException("You not allow to toggle this function");
        }

        if(restaurant.getStatus().equals(RestaurantStatus.PENDING)){
            throw new RuntimeException("This is not available for longer time");
        }
        if(restaurant.getStatus().equals(RestaurantStatus.OPEN)){
            restaurant.setStatus(RestaurantStatus.CLOSED);
        }
        else{
            restaurant.setStatus(RestaurantStatus.OPEN);
        }
        Restaurant updated = restRepo.save(restaurant);
        return new RestaurantResponse(
                updated.getId(),updated.getName(),updated.getCity(),updated.getRating(),updated.getStatus().name(),updated.getCuisine(),updated.getCreatedAt(), updated.getOwner().getName()
        );
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restRepo.findAll().stream()
                .map(r -> new RestaurantResponse(r.getId(), r.getName(), r.getCity(), r.getRating(), r.getStatus().name(), r.getCuisine(), r.getCreatedAt(), r.getOwner().getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveRestaurant(Long id) {
        Restaurant restaurant = restRepo.findById(id).orElseThrow(() -> new RuntimeException("Restaurant not found"));
        restaurant.setStatus(RestaurantStatus.OPEN);
        restRepo.save(restaurant);
    }
}
