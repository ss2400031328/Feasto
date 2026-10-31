package com.klef.sdp.dto;

import com.klef.sdp.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest
{
    @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
    private String name;

    @Email(message = "Invalid email format")
    @Pattern(regexp = ".*\\S.*", message = "Email must not be blank")
    private String email;

    @Pattern(regexp = ".*\\S.*", message = "Password must not be blank")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    private String phone;

    @Pattern(regexp = ".*\\S.*", message = "Address must not be blank")
    private String address;

    private Role role;

    @Pattern(regexp = ".*\\S.*", message = "Region must not be blank")
    private String region;
}