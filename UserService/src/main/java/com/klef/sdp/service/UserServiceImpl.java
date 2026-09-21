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
import com.klef.sdp.dto.UserResponse;
import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;
import com.klef.sdp.exception.ResourceNotFoundException;
import com.klef.sdp.exception.UnauthorizedException;
import com.klef.sdp.repository.UserRepository;
import com.klef.sdp.security.JwtUtil;

@Service
public class UserServiceImpl implements UserService
{
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(
        UserRepository repository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    )
    {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserResponse register(RegisterRequest request)
    {
        if (repository.existsByEmail(request.getEmail()))
        {
            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        if (repository.existsByPhone(request.getPhone()))
        {
            throw new IllegalArgumentException(
                "Phone number is already registered"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                    passwordEncoder.encode(request.getPassword())
                )
                .phone(request.getPhone())
                .address(request.getAddress())
                .region(request.getRegion())
                .role(Role.CUSTOMER)
                .build();

        return mapToResponse(repository.save(user));
    }

    @Override
    public UserResponse createAdminUser(
        RegisterRequest request,
        Authentication authentication
    )
    {
        if (!hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            throw new AccessDeniedException(
                "Only SUPER_ADMIN can create administrative users"
            );
        }

        if (repository.existsByEmail(request.getEmail()))
        {
            throw new IllegalArgumentException(
                "Email is already registered"
            );
        }

        if (repository.existsByPhone(request.getPhone()))
        {
            throw new IllegalArgumentException(
                "Phone number is already registered"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                    passwordEncoder.encode(request.getPassword())
                )
                .phone(request.getPhone())
                .address(request.getAddress())
                .region(request.getRegion())
                .role(
                    request.getRole() != null
                        ? request.getRole()
                        : Role.CUSTOMER
                )
                .build();

        return mapToResponse(repository.save(user));
    }

    @Override
    public LoginResponse login(LoginRequest request)
    {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                        "User not found with email: "
                        + request.getEmail()
                    )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        ))
        {
            throw new UnauthorizedException(
                "Invalid email or password"
            );
        }

        UserDetails userDetails =
                loadUserByUsername(user.getEmail());

        String token =
                jwtUtil.generateToken(userDetails);

        return LoginResponse.builder()
                .token(token)
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
    public List<UserResponse> getAllUsers(
        Authentication authentication,
        String region
    )
    {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            if (region != null && !region.isBlank())
            {
                return repository
                        .findByRegionIgnoreCase(region)
                        .stream()
                        .map(this::mapToResponse)
                        .toList();
            }

            return repository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (hasRole(authentication, "ROLE_REGIONAL_MANAGER"))
        {
            User manager = getCurrentUser(authentication);

            if (manager.getRegion() == null)
            {
                return Collections.emptyList();
            }

            return repository
                    .findByRegionIgnoreCase(manager.getRegion())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        throw new AccessDeniedException(
            "Access denied"
        );
    }

    @Override
    public UserResponse getUserById(
        Long id,
        Authentication authentication
    )
    {
        User user = repository.findById(id)
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                        "User not found with id: " + id
                    )
                );

        User currentUser = getCurrentUser(authentication);

        if (hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            return mapToResponse(user);
        }

        if (hasRole(authentication, "ROLE_REGIONAL_MANAGER"))
        {
            if (!sameRegion(currentUser, user))
            {
                throw new AccessDeniedException(
                    "You can only access users in your region"
                );
            }

            return mapToResponse(user);
        }

        if (hasRole(authentication, "ROLE_CUSTOMER"))
        {
            if (!currentUser.getId().equals(user.getId()))
            {
                throw new AccessDeniedException(
                    "You can only access your own profile"
                );
            }

            return mapToResponse(user);
        }

        throw new AccessDeniedException(
            "Access denied"
        );
    }

    @Override
    public UserResponse getUserByEmail(
        String email,
        Authentication authentication
    )
    {
        User user = repository.findByEmail(email)
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                        "User not found with email: " + email
                    )
                );

        User currentUser = getCurrentUser(authentication);

        if (hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            return mapToResponse(user);
        }

        if (hasRole(authentication, "ROLE_REGIONAL_MANAGER"))
        {
            if (!sameRegion(currentUser, user))
            {
                throw new AccessDeniedException(
                    "You can only access users in your region"
                );
            }

            return mapToResponse(user);
        }

        if (!currentUser.getId().equals(user.getId()))
        {
            throw new AccessDeniedException(
                "You can only access your own profile"
            );
        }

        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getUsersByRegion(
        String region,
        Authentication authentication
    )
    {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            return repository
                    .findByRegionIgnoreCase(region)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        User manager = getCurrentUser(authentication);

        if (hasRole(authentication, "ROLE_REGIONAL_MANAGER")
                && sameRegion(manager, region))
        {
            return repository
                    .findByRegionIgnoreCase(region)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        throw new AccessDeniedException(
            "You are not authorized to access this region"
        );
    }

    @Override
    public UserResponse updateUser(
        Long id,
        UpdateUserRequest request,
        Authentication authentication
    )
    {
        User user = repository.findById(id)
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                        "User not found with id: " + id
                    )
                );

        User currentUser = getCurrentUser(authentication);

        boolean superAdmin =
                hasRole(authentication, "ROLE_SUPER_ADMIN");

        boolean regionalManager =
                hasRole(authentication, "ROLE_REGIONAL_MANAGER");

        boolean customer =
                hasRole(authentication, "ROLE_CUSTOMER");

        if (!superAdmin)
        {
            if (regionalManager && !sameRegion(currentUser, user))
            {
                throw new AccessDeniedException(
                    "You can only update users in your region"
                );
            }

            if (customer &&
                !currentUser.getId().equals(user.getId()))
            {
                throw new AccessDeniedException(
                    "You can only update your own profile"
                );
            }

            request.setRole(null);
            request.setRegion(null);
        }

        if (request.getName() != null)
            user.setName(request.getName());

        if (request.getEmail() != null &&
            !request.getEmail().equalsIgnoreCase(user.getEmail()))
        {
            if (repository.existsByEmail(request.getEmail()))
            {
                throw new IllegalArgumentException(
                    "Email is already registered"
                );
            }

            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null)
        {
            user.setPassword(
                passwordEncoder.encode(request.getPassword())
            );
        }

        if (request.getPhone() != null &&
            !request.getPhone().equals(user.getPhone()))
        {
            if (repository.existsByPhone(request.getPhone()))
            {
                throw new IllegalArgumentException(
                    "Phone number is already registered"
                );
            }

            user.setPhone(request.getPhone());
        }

        if (request.getAddress() != null)
            user.setAddress(request.getAddress());

        if (request.getRole() != null)
            user.setRole(request.getRole());

        if (request.getRegion() != null)
            user.setRegion(request.getRegion());

        return mapToResponse(repository.save(user));
    }

    @Override
    public void deleteUser(
        Long id,
        Authentication authentication
    )
    {
        if (!hasRole(authentication, "ROLE_SUPER_ADMIN"))
        {
            throw new AccessDeniedException(
                "Only SUPER_ADMIN can delete users"
            );
        }

        if (!repository.existsById(id))
        {
            throw new ResourceNotFoundException(
                "User not found with id: " + id
            );
        }

        repository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(
        String username
    )
    {
        User user = repository.findByEmail(username)
                .orElseThrow(
                    () -> new UsernameNotFoundException(
                        "User not found with email: " + username
                    )
                );

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(
                    new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                    )
                )
        );
    }

    private User getCurrentUser(
        Authentication authentication
    )
    {
        return repository
                .findByEmail(authentication.getName())
                .orElseThrow(
                    () -> new ResourceNotFoundException(
                        "Current user not found"
                    )
                );
    }

    private boolean hasRole(
        Authentication authentication,
        String role
    )
    {
        return authentication != null
                && authentication.getAuthorities()
                    .stream()
                    .anyMatch(
                        authority ->
                            authority.getAuthority()
                                .equalsIgnoreCase(role)
                    );
    }

    private boolean sameRegion(
        User first,
        User second
    )
    {
        return first.getRegion() != null
                && second.getRegion() != null
                && first.getRegion()
                    .equalsIgnoreCase(second.getRegion());
    }

    private boolean sameRegion(
        User user,
        String region
    )
    {
        return user.getRegion() != null
                && region != null
                && user.getRegion()
                    .equalsIgnoreCase(region);
    }

    private UserResponse mapToResponse(User user)
    {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .region(user.getRegion())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}