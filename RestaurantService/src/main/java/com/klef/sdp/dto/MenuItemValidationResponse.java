package com.klef.sdp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemValidationResponse {

    private Long menuItemId;

    private Long restaurantId;

    private String name;

    private Double price;

    private Boolean available;
}