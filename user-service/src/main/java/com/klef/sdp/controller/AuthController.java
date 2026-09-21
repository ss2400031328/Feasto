package com.klef.sdp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
@Validated
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication & Registration APIs")
public class AuthController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(service.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User login & JWT token retrieval")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }
}
