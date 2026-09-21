package com.klef.sdp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
@Validated
@RequestMapping("/users")
@Tag(name = "User Management", description = "User profile, regional management, and administrative APIs")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER', 'CUSTOMER')")
    @Operation(summary = "View own profile")
    public ResponseEntity<UserResponse> getSelfProfile(Authentication authentication) {
        return ResponseEntity.ok(service.getUserByEmail(authentication.getName(), authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER')")
    @Operation(summary = "Get all users (Super Admin views all, Regional Manager views own region only)")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(value = "region", required = false) String region,
            Authentication authentication) {
        return ResponseEntity.ok(service.getAllUsers(authentication, region));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get user by ID (scoped by permissions and region)")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(service.getUserById(id, authentication));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER')")
    @Operation(summary = "Lookup user by email (Super Admin global, Regional Manager region-scoped)")
    public ResponseEntity<UserResponse> getUserByEmail(
            @PathVariable String email,
            Authentication authentication) {
        return ResponseEntity.ok(service.getUserByEmail(email, authentication));
    }

    @GetMapping("/region/{region}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER')")
    @Operation(summary = "Get users by specific region")
    public ResponseEntity<List<UserResponse>> getUsersByRegion(
            @PathVariable String region,
            Authentication authentication) {
        return ResponseEntity.ok(service.getUsersByRegion(region, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER', 'CUSTOMER')")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.updateUser(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user by ID (SUPER_ADMIN only)")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        service.deleteUser(id, authentication);
        return ResponseEntity.ok("User deleted successfully with ID: " + id);
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Regional Manager or Administrative User (SUPER_ADMIN only)")
    public ResponseEntity<UserResponse> createAdminUser(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {
        return new ResponseEntity<>(service.createAdminUser(request, authentication), HttpStatus.CREATED);
    }
}