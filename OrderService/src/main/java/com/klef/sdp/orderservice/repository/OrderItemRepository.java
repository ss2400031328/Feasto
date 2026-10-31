package com.klef.sdp.orderservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.sdp.orderservice.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderOrderId(Long orderId);
}