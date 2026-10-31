package com.klef.sdp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuItemId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "restaurant_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_menu_restaurant")
    )
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

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
