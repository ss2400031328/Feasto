package com.klef.sdp.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserRequest;
import com.klef.sdp.dto.UserResponse;

public interface UserService {
    // Standard public registration (always creates CUSTOMER) & login
    UserResponse register(RegisterRequest request);

    // Administrative user creation (SUPER_ADMIN only)
    UserResponse createAdminUser(RegisterRequest request, Authentication authentication);

    LoginResponse login(LoginRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request, Authentication authentication);

    // Overloads for compatibility
    UserResponse saveUser(RegisterRequest request);

    UserResponse saveUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    LoginResponse userLogin(LoginRequest request);

    // Read & Delete methods
    List<UserResponse> getAllUsers();

    List<UserResponse> getAllUsers(Authentication authentication, String regionFilter);

    UserResponse getUserById(Long id);

    UserResponse getUserById(Long id, Authentication authentication);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByEmail(String email, Authentication authentication);

    List<UserResponse> getUsersByRegion(String region, Authentication authentication);

    void deleteUser(Long id);

    void deleteUser(Long id, Authentication authentication);

    UserDetails loadUserByUsername(String username);
}