package com.klef.sdp.orderservice.dto;

import java.time.LocalDateTime;

import com.klef.sdp.orderservice.entity.OrderStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {

    private Long historyId;
    private OrderStatus status;
    private LocalDateTime changedAt;
}