package com.klef.sdp.service;

import java.util.List;

import com.klef.sdp.dto.AvailabilityRequest;
import com.klef.sdp.dto.MenuItemRequest;
import com.klef.sdp.dto.MenuItemResponse;
import com.klef.sdp.dto.MenuItemSearchResponse;
import com.klef.sdp.dto.MenuItemValidationResponse;

public interface MenuService {

    MenuItemResponse create(
            Long restaurantId,
            MenuItemRequest request);

    List<MenuItemResponse> getByRestaurant(
            Long restaurantId);

    MenuItemResponse getById(
            Long menuItemId);

    MenuItemResponse update(
            Long menuItemId,
            MenuItemRequest request);

    void delete(Long menuItemId);

    List<MenuItemSearchResponse> searchFood(
            String name);

    MenuItemResponse updateAvailability(
            Long menuItemId,
            AvailabilityRequest request);

    MenuItemValidationResponse validateMenuItem(
            Long menuItemId);
}
