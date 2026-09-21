package com.klef.sdp.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klef.sdp.orderservice.dto.MenuItemValidationResponse;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    @GetMapping("/menu-item/{menuItemId}/validate")
    MenuItemValidationResponse validateMenuItem(
            @PathVariable Long menuItemId);
}
