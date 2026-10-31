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
public class RestaurantResponse {

    private Long restaurantId;
    private String name;
    private String location;
    private String region;
    private String address;
    private String phone;
    private Double rating;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}