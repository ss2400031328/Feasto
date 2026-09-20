package com.klef.sdp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.klef.sdp.dto.RestaurantRequest;
import com.klef.sdp.dto.RestaurantResponse;
import com.klef.sdp.entity.Restaurant;
import com.klef.sdp.repository.RestaurantRepository;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository repo;

    public RestaurantServiceImpl(RestaurantRepository repo) {
        this.repo = repo;
    }

    // CREATE RESTAURANT
    @Override
    public RestaurantResponse create(RestaurantRequest request) {

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .location(request.getLocation())
                .region(request.getRegion())
                .address(request.getAddress())
                .phone(request.getPhone())
                .rating(request.getRating())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Restaurant saved = repo.save(restaurant);

        return mapToResponse(saved);
    }

    // GET ALL RESTAURANTS
    @Override
    public List<RestaurantResponse> getAll() {

        return repo.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET RESTAURANT BY ID
    @Override
    public RestaurantResponse getById(Long restaurantId) {

        Restaurant restaurant = repo.findById(restaurantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Restaurant not found with id: "
                                        + restaurantId));

        return mapToResponse(restaurant);
    }

    // UPDATE RESTAURANT
    @Override
    public RestaurantResponse update(
            Long restaurantId,
            RestaurantRequest request) {

        Restaurant restaurant = repo.findById(restaurantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Restaurant not found with id: "
                                        + restaurantId));

        restaurant.setName(request.getName());
        restaurant.setLocation(request.getLocation());
        restaurant.setRegion(request.getRegion());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setRating(request.getRating());

        // Only updatedAt changes during update
        restaurant.setUpdatedAt(LocalDateTime.now());

        Restaurant updated = repo.save(restaurant);

        return mapToResponse(updated);
    }

    // DELETE RESTAURANT
    @Override
    public void delete(Long restaurantId) {

        Restaurant restaurant = repo.findById(restaurantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Restaurant not found with id: "
                                        + restaurantId));

        repo.delete(restaurant);
    }

    // SEARCH BY NAME
    @Override
    public List<RestaurantResponse> searchByName(
            String name) {

        return repo.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // SEARCH BY LOCATION
    @Override
    public List<RestaurantResponse> searchByLocation(
            String location) {

        return repo.findByLocationContainingIgnoreCase(location)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ENTITY → RESPONSE DTO
    private RestaurantResponse mapToResponse(
            Restaurant restaurant) {

        return RestaurantResponse.builder()
                .restaurantId(
                        restaurant.getRestaurantId())
                .name(
                        restaurant.getName())
                .location(
                        restaurant.getLocation())
                .region(
                        restaurant.getRegion())
                .address(
                        restaurant.getAddress())
                .phone(
                        restaurant.getPhone())
                .rating(
                        restaurant.getRating())
                .active(
                        restaurant.getActive())
                .createdAt(
                        restaurant.getCreatedAt())
                .updatedAt(
                        restaurant.getUpdatedAt())
                .build();
    }
}