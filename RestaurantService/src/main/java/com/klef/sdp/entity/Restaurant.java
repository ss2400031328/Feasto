package com.klef.sdp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "restaurants",
		uniqueConstraints = {
		@UniqueConstraint(columnNames = "phone")
    	}
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;
    
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column
    private Double rating;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}