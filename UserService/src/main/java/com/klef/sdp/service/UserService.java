package com.klef.sdp.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserResponse;

public interface UserService
{
    UserResponse register(RegisterRequest request);

    UserResponse createAdminUser(
        RegisterRequest request,
        Authentication authentication
    );

    LoginResponse login(LoginRequest request);

    List<UserResponse> getAllUsers(
        Authentication authentication,
        String region
    );

    UserResponse getUserById(
        Long id,
        Authentication authentication
    );

    UserResponse getUserByEmail(
        String email,
        Authentication authentication
    );

    List<UserResponse> getUsersByRegion(
        String region,
        Authentication authentication
    );

    UserResponse updateUser(
        Long id,
        UpdateUserRequest request,
        Authentication authentication
    );

    void deleteUser(
        Long id,
        Authentication authentication
    );

    UserDetails loadUserByUsername(String username);
}