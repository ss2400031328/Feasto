package com.klef.sdp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MenuItemRequest {

    @NotBlank(message = "Menu item name is required")
    @Size(min = 2, max = 100,
          message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500,
          message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 50,
          message = "Category must be between 2 and 50 characters")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01",
        message = "Price must be greater than 0")
    private Double price;

    @Builder.Default
    private Boolean available = true;
}