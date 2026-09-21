package com.klef.sdp.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemValidationResponse {

    private Long menuItemId;
    private Long restaurantId;
    private String name;
    private Double price;
    private Boolean available;
}
