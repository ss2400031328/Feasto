package com.klef.sdp.service;

import java.util.Collections;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.klef.sdp.dto.LoginRequest;
import com.klef.sdp.dto.LoginResponse;
import com.klef.sdp.dto.RegisterRequest;
import com.klef.sdp.dto.UpdateUserRequest;
import com.klef.sdp.dto.UserRequest;
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;
import com.klef.sdp.exception.ResourceNotFoundException;
import com.klef.sdp.exception.UnauthorizedException;
import com.klef.sdp.repository.UserRepository;
import com.klef.sdp.security.JwtUtil;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Public Registration: MUST ALWAYS create a CUSTOMER role.
     * Normal public registration MUST NOT allow creation of REGIONAL_MANAGER or SUPER_ADMIN.
     */
    @Override
    public UserResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank() && repository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number is already registered: " + phone);
        }

        // Public registration ALWAYS forces CUSTOMER role
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .role(Role.CUSTOMER)
                .region(request.getRegion())
                .address(request.getAddress() != null ? request.getAddress() : "")
                .build();

        User savedUser = repository.save(user);
        return mapToUserResponse(savedUser, null);
    }

    /**
     * Admin User Creation: SUPER_ADMIN can create users with specific roles (REGIONAL_MANAGER, SUPER_ADMIN, CUSTOMER)
     * and assign regions.
     */
    @Override
    public UserResponse createAdminUser(RegisterRequest request, Authentication authentication) {
        if (authentication == null || !hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            throw new AccessDeniedException("Access denied: Only SUPER_ADMIN can create administrative users");
        }

        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank() && repository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number is already registered: " + phone);
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .role(assignedRole)
                .region(request.getRegion())
                .address(request.getAddress() != null ? request.getAddress() : "")
                .build();

        User savedUser = repository.save(user);
        return mapToUserResponse(savedUser, null);
    }

    @Override
    public UserResponse saveUser(RegisterRequest request) {
        return register(request);
    }

    @Override
    public UserResponse saveUser(UserRequest request) {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(request.getRole())
                .region(request.getRegion())
                .build();
        return register(registerRequest);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .region(user.getRegion())
                .address(user.getAddress())
                .message("Login successful")
                .build();
    }

    @Override
    public LoginResponse userLogin(LoginRequest request) {
        return login(request);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return repository.findAll()
                .stream()
                .map(user -> mapToUserResponse(user, null))
                .toList();
    }

    @Override
    public List<UserResponse> getAllUsers(Authentication authentication, String regionFilter) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return getAllUsers();
        }

        boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
        boolean isRegionalManager = hasRole(authentication, "ROLE_REGIONAL_MANAGER");

        if (isSuperAdmin) {
            if (regionFilter != null && !regionFilter.isBlank()) {
                return repository.findByRegionIgnoreCase(regionFilter)
                        .stream()
                        .map(user -> mapToUserResponse(user, null))
                        .toList();
            }
            return getAllUsers();
        }

        if (isRegionalManager) {
            User manager = getCallerUser(authentication);
            String managerRegion = manager != null ? manager.getRegion() : null;
            if (managerRegion == null || managerRegion.isBlank()) {
                return Collections.emptyList();
            }

            if (regionFilter != null && !regionFilter.isBlank() && !regionFilter.equalsIgnoreCase(managerRegion)) {
                throw new AccessDeniedException("Access denied: You are only authorized to access users in region: " + managerRegion);
            }

            return repository.findByRegionIgnoreCase(managerRegion)
                    .stream()
                    .map(user -> mapToUserResponse(user, null))
                    .toList();
        }

        throw new AccessDeniedException("Access denied: Insufficient privileges");
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user, null);
    }

    @Override
    public UserResponse getUserById(Long id, Authentication authentication) {
        User targetUser = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
            boolean isRegionalManager = hasRole(authentication, "ROLE_REGIONAL_MANAGER");
            boolean isCustomer = hasRole(authentication, "ROLE_CUSTOMER");

            if (isSuperAdmin) {
                return mapToUserResponse(targetUser, null);
            }

            User caller = getCallerUser(authentication);

            if (isRegionalManager) {
                String managerRegion = caller != null ? caller.getRegion() : null;
                if (managerRegion == null || targetUser.getRegion() == null || !managerRegion.equalsIgnoreCase(targetUser.getRegion())) {
                    throw new AccessDeniedException("Access denied: You are only authorized to access users in region: " + managerRegion);
                }
            } else if (isCustomer) {
                if (caller == null || !targetUser.getId().equals(caller.getId())) {
                    throw new AccessDeniedException("Access denied: You can only view your own profile");
                }
            }
        }

        return mapToUserResponse(targetUser, null);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user, null);
    }

    @Override
    public UserResponse getUserByEmail(String email, Authentication authentication) {
        User targetUser = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
            boolean isRegionalManager = hasRole(authentication, "ROLE_REGIONAL_MANAGER");
            boolean isCustomer = hasRole(authentication, "ROLE_CUSTOMER");

            if (isSuperAdmin) {
                return mapToUserResponse(targetUser, null);
            }

            User caller = getCallerUser(authentication);

            if (isRegionalManager) {
                String managerRegion = caller != null ? caller.getRegion() : null;
                if (managerRegion == null || targetUser.getRegion() == null || !managerRegion.equalsIgnoreCase(targetUser.getRegion())) {
                    throw new AccessDeniedException("Access denied: You are only authorized to access users in region: " + managerRegion);
                }
            } else if (isCustomer) {
                if (caller == null || !targetUser.getId().equals(caller.getId())) {
                    throw new AccessDeniedException("Access denied: You can only view your own profile");
                }
            }
        }

        return mapToUserResponse(targetUser, null);
    }

    @Override
    public List<UserResponse> getUsersByRegion(String region, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
            boolean isRegionalManager = hasRole(authentication, "ROLE_REGIONAL_MANAGER");

            if (isRegionalManager && !isSuperAdmin) {
                User manager = getCallerUser(authentication);
                String managerRegion = manager != null ? manager.getRegion() : null;
                if (managerRegion == null || !managerRegion.equalsIgnoreCase(region)) {
                    throw new AccessDeniedException("Access denied: You are only authorized to access users in region: " + managerRegion);
                }
            } else if (!isSuperAdmin) {
                throw new AccessDeniedException("Access denied: Insufficient privileges");
            }
        }

        return repository.findByRegionIgnoreCase(region)
                .stream()
                .map(user -> mapToUserResponse(user, null))
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (repository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank() && !phone.equals(user.getPhone())) {
            if (repository.existsByPhone(phone)) {
                throw new IllegalArgumentException("Phone number is already registered: " + phone);
            }
            user.setPhone(phone);
        }

        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updatedUser = repository.save(user);
        return mapToUserResponse(updatedUser, null);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, Authentication authentication) {
        User targetUser = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
            boolean isRegionalManager = hasRole(authentication, "ROLE_REGIONAL_MANAGER");
            boolean isCustomer = hasRole(authentication, "ROLE_CUSTOMER");

            User caller = getCallerUser(authentication);

            // Matrix rule: Change own role -> NO, Change own region -> NO (for all roles including SUPER_ADMIN)
            boolean isSelfUpdate = (caller != null && caller.getId().equals(targetUser.getId()));
            if (isSelfUpdate) {
                request.setRole(null);
                request.setRegion(null);
            }

            if (isRegionalManager && !isSuperAdmin) {
                String managerRegion = caller != null ? caller.getRegion() : null;
                if (managerRegion == null || targetUser.getRegion() == null || !managerRegion.equalsIgnoreCase(targetUser.getRegion())) {
                    throw new AccessDeniedException("Access denied: You are only authorized to update users in region: " + managerRegion);
                }
                // REGIONAL_MANAGER cannot change role or region of any user
                request.setRole(null);
                request.setRegion(null);
            } else if (isCustomer && !isSuperAdmin) {
                if (caller == null || !targetUser.getId().equals(caller.getId())) {
                    throw new AccessDeniedException("Access denied: You can only update your own profile");
                }
                // CUSTOMER cannot change role or region
                request.setRole(null);
                request.setRegion(null);
            }
        }

        return updateUser(id, request);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(request.getRole())
                .region(request.getRegion())
                .build();
        return updateUser(id, updateRequest);
    }

    @Override
    public void deleteUser(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Delete users capability matrix:
     * CUSTOMER: NO
     * REGIONAL_MANAGER: NO
     * SUPER_ADMIN: YES
     */
    @Override
    public void deleteUser(Long id, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isSuperAdmin = hasRole(authentication, "ROLE_SUPER_ADMIN");
            if (!isSuperAdmin) {
                throw new AccessDeniedException("Access denied: Only SUPER_ADMIN is authorized to delete users");
            }
        }

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        repository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority));
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(roleName));
    }

    private User getCallerUser(Authentication authentication) {
        return repository.findByEmail(authentication.getName()).orElse(null);
    }

    private UserResponse mapToUserResponse(User user, String token) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .region(user.getRegion())
                .address(user.getAddress())
                .token(token)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
