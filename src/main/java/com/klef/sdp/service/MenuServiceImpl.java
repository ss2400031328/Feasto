package com.klef.sdp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.klef.sdp.dto.AvailabilityRequest;
import com.klef.sdp.dto.MenuItemRequest;
import com.klef.sdp.dto.MenuItemResponse;
import com.klef.sdp.dto.MenuItemSearchResponse;
import com.klef.sdp.entity.MenuItem;
import com.klef.sdp.entity.Restaurant;
import com.klef.sdp.repository.MenuRepository;
import com.klef.sdp.repository.RestaurantRepository;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepo;
    private final RestaurantRepository restaurantRepo;

    public MenuServiceImpl(
            MenuRepository menuRepo,
            RestaurantRepository restaurantRepo) {

        this.menuRepo = menuRepo;
        this.restaurantRepo = restaurantRepo;
    }

    // CREATE MENU ITEM
    @Override
    public MenuItemResponse create(
            Long restaurantId,
            MenuItemRequest request) {

        // First check whether restaurant exists
        Restaurant restaurant = restaurantRepo
                .findById(restaurantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Restaurant not found with id: "
                                        + restaurantId));

        // Create menu item
        MenuItem menuItem = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .available(request.getAvailable())
                .build();

        MenuItem saved = menuRepo.save(menuItem);

        return mapToResponse(saved);
    }

    // GET ALL MENU ITEMS OF A RESTAURANT
    @Override
    public List<MenuItemResponse> getByRestaurant(
            Long restaurantId) {

        if (!restaurantRepo.existsById(restaurantId)) {
            throw new RuntimeException(
                    "Restaurant not found with id: "
                            + restaurantId);
        }

        return menuRepo
                .findByRestaurantRestaurantId(restaurantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET MENU ITEM BY ID
    @Override
    public MenuItemResponse getById(Long menuItemId) {

        MenuItem menuItem = menuRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Menu item not found with id: "
                                        + menuItemId));

        return mapToResponse(menuItem);
    }

    // UPDATE MENU ITEM
    @Override
    public MenuItemResponse update(
            Long menuItemId,
            MenuItemRequest request) {

        MenuItem menuItem = menuRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Menu item not found with id: "
                                        + menuItemId));

        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setCategory(request.getCategory());
        menuItem.setPrice(request.getPrice());
        menuItem.setAvailable(request.getAvailable());

        MenuItem updated = menuRepo.save(menuItem);

        return mapToResponse(updated);
    }

    // DELETE MENU ITEM
    @Override
    public void delete(Long menuItemId) {

        MenuItem menuItem = menuRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Menu item not found with id: "
                                        + menuItemId));

        menuRepo.delete(menuItem);
    }

    // SEARCH FOOD
    @Override
    public List<MenuItemSearchResponse> searchFood(
            String name) {

        return menuRepo
                .findByNameContainingIgnoreCase(name)
                .stream()
                .map(menuItem ->
                        MenuItemSearchResponse.builder()
                                .menuItemId(
                                        menuItem.getMenuItemId())

                                .restaurantId(
                                        menuItem.getRestaurant()
                                                .getRestaurantId())

                                .restaurantName(
                                        menuItem.getRestaurant()
                                                .getName())

                                .location(
                                        menuItem.getRestaurant()
                                                .getLocation())

                                .name(menuItem.getName())

                                .category(
                                        menuItem.getCategory())

                                .price(menuItem.getPrice())

                                .available(
                                        menuItem.getAvailable())

                                .build())
                .toList();
    }

    // UPDATE AVAILABILITY
    @Override
    public MenuItemResponse updateAvailability(
            Long menuItemId,
            AvailabilityRequest request) {

        MenuItem menuItem = menuRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Menu item not found with id: "
                                        + menuItemId));

        menuItem.setAvailable(request.getAvailable());

        MenuItem updated = menuRepo.save(menuItem);

        return mapToResponse(updated);
    }

    // ENTITY -> RESPONSE DTO
    private MenuItemResponse mapToResponse(
            MenuItem menuItem) {
        return MenuItemResponse.builder()
                .menuItemId(menuItem.getMenuItemId())
                .restaurantId(menuItem.getRestaurant()
                                .getRestaurantId())

                .name(menuItem.getName())

                .description(
                        menuItem.getDescription())

                .category(
                        menuItem.getCategory())

                .price(
                        menuItem.getPrice())

                .available(
                        menuItem.getAvailable())

                .createdAt(
                        menuItem.getCreatedAt())

                .updatedAt(
                        menuItem.getUpdatedAt())

                .build();
    }
}