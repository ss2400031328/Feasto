package com.klef.sdp.service;

import java.util.List;

import com.klef.sdp.dto.RestaurantRequest;
import com.klef.sdp.dto.RestaurantResponse;

public interface RestaurantService {

    RestaurantResponse create(RestaurantRequest request);

    List<RestaurantResponse> getAll();

    RestaurantResponse getById(Long restaurantId);

    RestaurantResponse update(
            Long restaurantId,
            RestaurantRequest request);

    RestaurantResponse setActive(Long restaurantId, boolean active);

    void delete(Long restaurantId);

    List<RestaurantResponse> searchByName(String name);

    List<RestaurantResponse> searchByLocation(String location);
}