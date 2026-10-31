package com.klef.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.sdp.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByNameContainingIgnoreCase(String name);

    List<Restaurant> findByLocationContainingIgnoreCase(  String location);

    boolean existsByPhone(String phone);
}