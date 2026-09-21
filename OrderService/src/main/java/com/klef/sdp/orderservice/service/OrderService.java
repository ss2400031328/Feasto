package com.klef.sdp.orderservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klef.sdp.orderservice.dto.CreateOrderRequest;
import com.klef.sdp.orderservice.dto.OrderItemRequest;
import com.klef.sdp.orderservice.dto.OrderItemResponse;
import com.klef.sdp.orderservice.dto.OrderResponse;
import com.klef.sdp.orderservice.dto.OrderTrackingResponse;
import com.klef.sdp.orderservice.entity.Order;
import com.klef.sdp.orderservice.entity.OrderItem;
import com.klef.sdp.orderservice.entity.OrderStatus;
import com.klef.sdp.orderservice.entity.OrderStatusHistory;
import com.klef.sdp.orderservice.exception.InvalidOrderStatusException;
import com.klef.sdp.orderservice.exception.OrderCancellationException;
import com.klef.sdp.orderservice.exception.OrderNotFoundException;
import com.klef.sdp.orderservice.repository.OrderItemRepository;
import com.klef.sdp.orderservice.repository.OrderRepository;
import com.klef.sdp.orderservice.repository.OrderStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    // create order method
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .userId(request.getUserId())
                .restaurantId(request.getRestaurantId())
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        for (OrderItemRequest itemRequest : request.getItems()) {

            BigDecimal subtotal = itemRequest.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemId(itemRequest.getItemId())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice())
                    .subtotal(subtotal)
                    .build();

            orderItemRepository.save(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PLACED)
                .changedAt(LocalDateTime.now())
                .build();

        orderStatusHistoryRepository.save(history);

        return toOrderResponse(order);
    }

    // get order by Id
    public OrderResponse getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found: " + orderId));

        return toOrderResponse(order);
    }

    // get order by user id
    public List<OrderResponse> getOrdersByUserId(Long userId) {

        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    // get order by restaurant id
    public List<OrderResponse> getOrdersByRestaurantId(Long restaurantId) {

        return orderRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    // get orders by status
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {

        return orderRepository.findByStatus(status)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    // get orders by tracking
    public List<OrderTrackingResponse> getOrderTracking(Long orderId) {

        orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found: " + orderId));

        return orderStatusHistoryRepository
                .findByOrderOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(history -> OrderTrackingResponse.builder()
                        .historyId(history.getHistoryId())
                        .status(history.getStatus())
                        .changedAt(history.getChangedAt())
                        .build())
                .toList();
    }

    // update the order by changing the status of order
    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from "
                            + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(newStatus)
                .changedAt(LocalDateTime.now())
                .build();

        orderStatusHistoryRepository.save(history);

        return toOrderResponse(savedOrder);
    }

    // validate the order status transition
    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus) {

        return switch (currentStatus) {

            case PLACED ->
                    newStatus == OrderStatus.CONFIRMED
                    || newStatus == OrderStatus.CANCELLED;

            case CONFIRMED ->
                    newStatus == OrderStatus.PREPARING
                    || newStatus == OrderStatus.CANCELLED;

            case PREPARING ->
                    newStatus == OrderStatus.READY_FOR_PICKUP;

            case READY_FOR_PICKUP ->
                    newStatus == OrderStatus.OUT_FOR_DELIVERY;

            case OUT_FOR_DELIVERY ->
                    newStatus == OrderStatus.DELIVERED;

            case DELIVERED, CANCELLED, PAYMENT_FAILED ->
                    false;
        };
    }

    // cancel the transaction
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PLACED
                && order.getStatus() != OrderStatus.CONFIRMED) {

            throw new OrderCancellationException(
                    "Order cannot be cancelled in status: "
                            + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.CANCELLED)
                .changedAt(LocalDateTime.now())
                .build();

        orderStatusHistoryRepository.save(history);

        return toOrderResponse(savedOrder);
    }

    // convert order entity into order response DTO
    private OrderResponse toOrderResponse(Order order) {

        List<OrderItem> items =
                orderItemRepository.findByOrderOrderId(order.getOrderId());

        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderItemResponse.builder()
                        .orderItemId(item.getOrderItemId())
                        .itemId(item.getItemId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}