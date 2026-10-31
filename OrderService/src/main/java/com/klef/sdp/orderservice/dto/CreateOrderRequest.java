package com.klef.sdp.orderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private Long userId;

    @NotNull
    @Positive
    private Long restaurantId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank
    private String paymentMethod;
}
