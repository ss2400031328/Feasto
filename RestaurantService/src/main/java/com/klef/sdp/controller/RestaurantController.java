package com.klef.sdp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klef.sdp.dto.RestaurantRequest;
import com.klef.sdp.dto.RestaurantResponse;
import com.klef.sdp.dto.RestaurantActivationRequest;
import com.klef.sdp.service.RestaurantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<RestaurantResponse> create( @RequestBody @Valid RestaurantRequest request) {
        RestaurantResponse response =service.create(request);
        return ResponseEntity
                .status(201)
                .body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<RestaurantResponse>> getAll() {

        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RestaurantResponse>> searchByName(
            @RequestParam String name) {

        return ResponseEntity.ok(
                service.searchByName(name));
    }

    @GetMapping("/location")
    public ResponseEntity<List<RestaurantResponse>> searchByLocation(
            @RequestParam String location) {

        return ResponseEntity.ok(
                service.searchByLocation(location));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> getById(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(
                service.getById(restaurantId));
    }

    @PutMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> update(
            @PathVariable Long restaurantId,
            @RequestBody @Valid RestaurantRequest request) {

        return ResponseEntity.ok(
                service.update(restaurantId, request));
    }

    @PatchMapping("/{restaurantId}/activation")
    public ResponseEntity<RestaurantResponse> setActive(
            @PathVariable Long restaurantId,
            @RequestBody @Valid RestaurantActivationRequest request) {
        return ResponseEntity.ok(
                service.setActive(restaurantId, request.getActive()));
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long restaurantId) {

        service.delete(restaurantId);

        return ResponseEntity.noContent().build();
    }
}