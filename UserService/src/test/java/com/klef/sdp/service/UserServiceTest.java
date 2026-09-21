package com.klef.sdp.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;
import com.klef.sdp.exception.ResourceNotFoundException;
import com.klef.sdp.repository.UserRepository;
import com.klef.sdp.security.JwtUtil;

class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        userService = new UserServiceImpl(repository, passwordEncoder, jwtUtil);
    }

    @Test
    void testRegisterUserSuccessCreatesCustomer() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Vardhan")
                .email("vardhan@example.com")
                .password("password123")
                .phone("9876543210")
                .address("Vijayawada")
                .role(Role.SUPER_ADMIN) // Public registration must ignore requested admin role
                .build();

        User savedUser = User.builder()
                .id(1L)
                .name("Vardhan")
                .email("vardhan@example.com")
                .password("encoded_pass")
                .phone("9876543210")
                .address("Vijayawada")
                .role(Role.CUSTOMER)
                .build();

        when(repository.existsByEmail("vardhan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(repository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("vardhan@example.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
    }

    @Test
    void testGetUserByIdSuccessForSelfCustomer() {
        User user = User.builder()
                .id(1L)
                .name("Vardhan")
                .email("vardhan@example.com")
                .role(Role.CUSTOMER)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "vardhan@example.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findByEmail("vardhan@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L, auth);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void testGetUserByIdDeniedForOtherCustomer() {
        User user = User.builder()
                .id(2L)
                .name("Other Customer")
                .email("other@example.com")
                .role(Role.CUSTOMER)
                .build();

        User caller = User.builder()
                .id(1L)
                .name("Vardhan")
                .email("vardhan@example.com")
                .role(Role.CUSTOMER)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "vardhan@example.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        when(repository.findById(2L)).thenReturn(Optional.of(user));
        when(repository.findByEmail("vardhan@example.com")).thenReturn(Optional.of(caller));

        assertThrows(AccessDeniedException.class, () -> userService.getUserById(2L, auth));
    }

    @Test
    void testGetUserByIdNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L, null));
    }

    @Test
    void testUserLoginSuccess() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("vardhan@example.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .name("Vardhan")
                .email("vardhan@example.com")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .build();

        when(repository.findByEmail("vardhan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(1L, response.getId());
        assertEquals("vardhan@example.com", response.getEmail());
    }

    @Test
    void testUpdateUserSuccessSelf() {
        User existingUser = User.builder()
                .id(1L)
                .name("Vardhan")
                .email("vardhan@example.com")
                .password("encoded_pass")
                .phone("9876543210")
                .address("Vijayawada")
                .role(Role.CUSTOMER)
                .build();

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name("Vardhan Updated")
                .address("Hyderabad")
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .name("Vardhan Updated")
                .email("vardhan@example.com")
                .password("encoded_pass")
                .phone("9876543210")
                .address("Hyderabad")
                .role(Role.CUSTOMER)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "vardhan@example.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        when(repository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(repository.findByEmail("vardhan@example.com")).thenReturn(Optional.of(existingUser));
        when(repository.save(any(User.class))).thenReturn(updatedUser);

        UserResponse response = userService.updateUser(1L, updateRequest, auth);

        assertNotNull(response);
        assertEquals("Vardhan Updated", response.getName());
        assertEquals("Hyderabad", response.getAddress());
    }

    @Test
    void testDeleteUserSuccessSuperAdminOnly() {
        User superAdmin = User.builder()
                .id(99L)
                .name("Super Admin")
                .email("admin@feasto.com")
                .role(Role.SUPER_ADMIN)
                .build();

        User targetUser = User.builder()
                .id(1L)
                .name("User to Delete")
                .email("user@example.com")
                .role(Role.CUSTOMER)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findById(1L)).thenReturn(Optional.of(targetUser));
        when(repository.findByEmail("admin@feasto.com")).thenReturn(Optional.of(superAdmin));

        userService.deleteUser(1L, auth);

        verify(repository).deleteById(1L);
    }

    @Test
    void testDeleteUserDeniedForRegionalManager() {
        User manager = User.builder()
                .id(10L)
                .name("Regional Manager")
                .email("manager.ap@feasto.com")
                .role(Role.REGIONAL_MANAGER)
                .region("AP")
                .build();

        User apUser = User.builder()
                .id(1L)
                .name("AP Customer")
                .email("ap.customer@feasto.com")
                .role(Role.CUSTOMER)
                .region("AP")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager.ap@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_REGIONAL_MANAGER")));

        when(repository.findById(1L)).thenReturn(Optional.of(apUser));
        when(repository.findByEmail("manager.ap@feasto.com")).thenReturn(Optional.of(manager));

        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(1L, auth));
    }

    @Test
    void testRegionalManagerGetsOnlyOwnRegionUsers() {
        User manager = User.builder()
                .id(10L)
                .name("Regional Manager")
                .email("manager.ap@feasto.com")
                .role(Role.REGIONAL_MANAGER)
                .region("AP")
                .build();

        User apUser = User.builder()
                .id(11L)
                .name("Customer AP")
                .email("ap.customer@feasto.com")
                .role(Role.CUSTOMER)
                .region("AP")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager.ap@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_REGIONAL_MANAGER")));

        when(repository.findByEmail("manager.ap@feasto.com")).thenReturn(Optional.of(manager));
        when(repository.findByRegionIgnoreCase("AP")).thenReturn(List.of(apUser));

        List<UserResponse> users = userService.getAllUsers(auth, null);

        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals("Customer AP", users.get(0).getName());
        assertEquals("AP", users.get(0).getRegion());
    }

    @Test
    void testRegionalManagerCannotAccessUserOfDifferentRegion() {
        User manager = User.builder()
                .id(10L)
                .name("Regional Manager")
                .email("manager.ap@feasto.com")
                .role(Role.REGIONAL_MANAGER)
                .region("AP")
                .build();

        User tsUser = User.builder()
                .id(20L)
                .name("Customer TS")
                .email("ts.customer@feasto.com")
                .role(Role.CUSTOMER)
                .region("TS")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager.ap@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_REGIONAL_MANAGER")));

        when(repository.findById(20L)).thenReturn(Optional.of(tsUser));
        when(repository.findByEmail("manager.ap@feasto.com")).thenReturn(Optional.of(manager));

        assertThrows(AccessDeniedException.class, () -> userService.getUserById(20L, auth));
    }

    @Test
    void testRegionalManagerCanAccessUserOfSameRegion() {
        User manager = User.builder()
                .id(10L)
                .name("Regional Manager")
                .email("manager.ap@feasto.com")
                .role(Role.REGIONAL_MANAGER)
                .region("AP")
                .build();

        User apUser = User.builder()
                .id(21L)
                .name("Customer AP")
                .email("ap.customer2@feasto.com")
                .role(Role.CUSTOMER)
                .region("AP")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "manager.ap@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_REGIONAL_MANAGER")));

        when(repository.findById(21L)).thenReturn(Optional.of(apUser));
        when(repository.findByEmail("manager.ap@feasto.com")).thenReturn(Optional.of(manager));

        UserResponse response = userService.getUserById(21L, auth);

        assertNotNull(response);
        assertEquals("Customer AP", response.getName());
        assertEquals("AP", response.getRegion());
    }

    @Test
    void testSuperAdminCanAccessAnyUser() {
        User superAdmin = User.builder()
                .id(99L)
                .name("Super Admin")
                .email("admin@feasto.com")
                .role(Role.SUPER_ADMIN)
                .build();

        User tsUser = User.builder()
                .id(30L)
                .name("Customer TS")
                .email("ts.customer3@feasto.com")
                .role(Role.CUSTOMER)
                .region("TS")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@feasto.com", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        when(repository.findById(30L)).thenReturn(Optional.of(tsUser));
        when(repository.findByEmail("admin@feasto.com")).thenReturn(Optional.of(superAdmin));

        UserResponse response = userService.getUserById(30L, auth);

        assertNotNull(response);
        assertEquals("Customer TS", response.getName());
        assertEquals("TS", response.getRegion());
    }
}
