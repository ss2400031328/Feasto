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
public class MenuItemSearchResponse {

    private Long menuItemId;

    private Long restaurantId;

    private String restaurantName;

    private String location;

    private String name;

    private String category;

    private Double price;

    private Boolean available;
}