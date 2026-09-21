package com.klef.sdp.orderservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderId;

	// this data from UserService Project
	private Long userId;

	// this data from RestaurantService Project
	private Long restaurantId;

	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
