package com.klef.sdp.dto;

import java.time.LocalDateTime;

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
public class MenuItemResponse {

    private Long menuItemId;
    private Long restaurantId;
    private String name;
    private String description;
    private String category;
    private Double price;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}