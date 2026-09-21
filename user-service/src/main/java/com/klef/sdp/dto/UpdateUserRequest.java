package com.klef.sdp.dto;

import com.klef.sdp.entity.Role;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String password;

    private String phone;

    private String address;

    private Role role;

    private String region;
}
