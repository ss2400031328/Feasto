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

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.service.UserService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

@Hidden
@CrossOrigin("*")
@RestController
@Validated
@RequestMapping("/user")
public class LegacyUserController {

    @Autowired
    private UserService service;

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("User Service is Running......!");
    }

    @PostMapping({"/add", "/register"})
    public ResponseEntity<UserResponse> saveUser(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(service.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> userLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @GetMapping("/displayall")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(value = "region", required = false) String region,
            Authentication authentication) {
        return ResponseEntity.ok(service.getAllUsers(authentication, region));
    }

    @GetMapping("/display/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER', 'CUSTOMER')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(service.getUserById(id, authentication));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_MANAGER', 'CUSTOMER')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.updateUser(id, request, authentication));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        service.deleteUser(id, authentication);
        return ResponseEntity.ok("User deleted successfully with ID: " + id);
    }

}
