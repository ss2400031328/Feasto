package com.klef.sdp.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.klef.sdp.orderservice.dto.CreateOrderRequest;
import com.klef.sdp.orderservice.dto.OrderResponse;
import com.klef.sdp.orderservice.dto.OrderTrackingResponse;
import com.klef.sdp.orderservice.entity.OrderStatus;
import com.klef.sdp.orderservice.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Create Order
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse order = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }

    // Get Order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    // Get orders of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                orderService.getOrdersByUserId(userId)
        );
    }

    // Get orders of a restaurant
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByRestaurantId(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(
                orderService.getOrdersByRestaurantId(restaurantId)
        );
    }

    // Get orders by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {

        return ResponseEntity.ok(
                orderService.getOrdersByStatus(status)
        );
    }

    // Update order status
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, status)
        );
    }

    // Cancel order
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                orderService.cancelOrder(orderId)
        );
    }

    // Get order tracking
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<List<OrderTrackingResponse>> getOrderTracking(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(orderService.getOrderTracking(orderId));
    }
}