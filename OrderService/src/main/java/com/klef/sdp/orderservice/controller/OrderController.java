package com.klef.sdp.orderservice.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    @GetMapping("/")
    public String home(){
        return "Order Service is up and running!";
    }

    // Create Order
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        request.setUserId(currentUserId(authentication));

        OrderResponse order = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }

    // Get Order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {

        OrderResponse order = orderService.getOrderById(orderId);
        requireOwnerOrAdmin(order, authentication);
        return ResponseEntity.ok(order);
    }

    // Get orders of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isSuperAdmin(authentication) && !userId.equals(currentUserId(authentication))) {
            throw new AccessDeniedException("You can only view your own orders");
        }

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
            @PathVariable Long orderId,
            Authentication authentication) {

        requireOwnerOrAdmin(orderService.getOrderById(orderId), authentication);
        return ResponseEntity.ok(
                orderService.cancelOrder(orderId)
        );
    }

    // Get order tracking
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<List<OrderTrackingResponse>> getOrderTracking(
            @PathVariable Long orderId,
            Authentication authentication) {

        requireOwnerOrAdmin(orderService.getOrderById(orderId), authentication);
        return ResponseEntity.ok(orderService.getOrderTracking(orderId));
    }

    private void requireOwnerOrAdmin(OrderResponse order, Authentication authentication) {
        if (!isSuperAdmin(authentication)
                && !Objects.equals(order.getUserId(), currentUserId(authentication))) {
            throw new AccessDeniedException("You can only access your own orders");
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object claim = jwtAuthentication.getToken().getClaim("userId");
            if (claim instanceof Number userId) {
                return userId.longValue();
            }
        }
        throw new AccessDeniedException("Authenticated token has no user identity");
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}