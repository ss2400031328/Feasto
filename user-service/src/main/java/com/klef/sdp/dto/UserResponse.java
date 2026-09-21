package com.klef.sdp.dto;

import java.time.LocalDateTime;

import com.klef.sdp.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String region;
    private String address;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}