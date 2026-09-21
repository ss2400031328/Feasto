package com.klef.sdp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klef.sdp.dto.AvailabilityRequest;
import com.klef.sdp.dto.MenuItemRequest;
import com.klef.sdp.dto.MenuItemResponse;
import com.klef.sdp.dto.MenuItemSearchResponse;
import com.klef.sdp.dto.MenuItemValidationResponse;
import com.klef.sdp.service.MenuService;

import jakarta.validation.Valid;

@RestController
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    // ADD MENU ITEM
    @PostMapping("/restaurant/{restaurantId}/menu")
    public ResponseEntity<MenuItemResponse> create(
            @PathVariable Long restaurantId,
            @RequestBody @Valid MenuItemRequest request) {

        MenuItemResponse response =
                service.create(
                        restaurantId,
                        request);

        return ResponseEntity
                .status(201)
                .body(response);
    }

    // GET RESTAURANT MENU
    @GetMapping("/restaurant/{restaurantId}/menu")
    public ResponseEntity<List<MenuItemResponse>> getMenu(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(
                service.getByRestaurant(
                        restaurantId));
    }

    // SEARCH FOOD
    @GetMapping("/menu-item/search")
    public ResponseEntity<List<MenuItemSearchResponse>> searchFood(
            @RequestParam String name) {

        return ResponseEntity.ok(
                service.searchFood(name));
    }

    // GET MENU ITEM
    @GetMapping("/menu-item/{menuItemId}")
    public ResponseEntity<MenuItemResponse> getById(
            @PathVariable Long menuItemId) {

        return ResponseEntity.ok(
                service.getById(menuItemId));
    }

    // INTERNAL VALIDATION FOR ORDER SERVICE
    @GetMapping("/menu-item/{menuItemId}/validate")
    public ResponseEntity<MenuItemValidationResponse> validateMenuItem(
            @PathVariable Long menuItemId) {

        return ResponseEntity.ok(
                service.validateMenuItem(menuItemId));
    }

    // UPDATE MENU ITEM
    @PutMapping("/menu-item/{menuItemId}")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable Long menuItemId,
            @RequestBody @Valid MenuItemRequest request) {

        return ResponseEntity.ok(
                service.update(
                        menuItemId,
                        request));
    }

    // DELETE MENU ITEM
    @DeleteMapping("/menu-item/{menuItemId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long menuItemId) {

        service.delete(menuItemId);

        return ResponseEntity
                .noContent()
                .build();
    }

    // UPDATE AVAILABILITY
    @PatchMapping(
            "/menu-item/{menuItemId}/availability")
    public ResponseEntity<MenuItemResponse>
            updateAvailability(
                    @PathVariable Long menuItemId,
                    @RequestBody @Valid
                    AvailabilityRequest request) {

        return ResponseEntity.ok(
                service.updateAvailability(
                        menuItemId,
                        request));
    }
}
