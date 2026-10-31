package com.klef.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.sdp.entity.MenuItem;

public interface MenuRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantRestaurantId( Long restaurantId);

    List<MenuItem> findByNameContainingIgnoreCase(String name);
}