package com.klef.sdp.orderservice.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long orderItemId;
    private Long itemId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}