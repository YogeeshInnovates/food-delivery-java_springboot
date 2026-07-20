package com.example.online_food_delivery.repository;

import com.example.online_food_delivery.model.MenuItems;
import com.example.online_food_delivery.model.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItems, Long> {
    List<MenuItems> findByRestaurantIdAndIsDeletedFalse(Long restaurantId);
    List<MenuItems> findByRestaurantIdAndCategoryAndIsDeletedFalse(Long restaurantId, String category);
    List<MenuItems> findByIsDeletedFalseAndIsAvailableTrue();
    Page<MenuItems> findByIsDeletedFalseAndIsAvailableTrue(Pageable pageable);

    @Query("SELECT m FROM MenuItems m JOIN m.restaurant r WHERE m.isDeleted = false AND m.isAvailable = true AND r.status IN :statuses")
    Page<MenuItems> findPopularByRestaurantStatuses(List<RestaurantStatus> statuses, Pageable pageable);

    List<MenuItems> findByIsDeletedFalse();
    List<MenuItems> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name);
    Page<MenuItems> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);
}
