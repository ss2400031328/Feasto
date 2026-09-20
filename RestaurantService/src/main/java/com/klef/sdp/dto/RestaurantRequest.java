package com.klef.sdp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 100,message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 100, message = "Location must be between 2 and 100 characters")
    private String location;

    @NotBlank(message = "Region is required")
    @Size(min = 2, max = 50, message = "Region must be between 2 and 50 characters")
    private String region;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[6-9][0-9]{9}$",
        message = "Enter a valid 10-digit phone number"
    )
    private String phone;

    @DecimalMin(value = "0.0",message = "Rating cannot be less than 0")
    @DecimalMax(value = "5.0",message = "Rating cannot be greater than 5")
    private Double rating;
}