package com.klef.sdp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;
import com.klef.sdp.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminEmail;
    private final String initialAdminPassword;
    private final String initialAdminPhone;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${FEASTO_INITIAL_ADMIN_EMAIL:}") String initialAdminEmail,
            @Value("${FEASTO_INITIAL_ADMIN_PASSWORD:}") String initialAdminPassword,
            @Value("${FEASTO_INITIAL_ADMIN_PHONE:}") String initialAdminPhone) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminEmail = initialAdminEmail;
        this.initialAdminPassword = initialAdminPassword;
        this.initialAdminPhone = initialAdminPhone;
    }

    @Override
    public void run(String... args) {
        if (initialAdminEmail.isBlank()
                && initialAdminPassword.isBlank()
                && initialAdminPhone.isBlank()) {
            return;
        }

        if (initialAdminEmail.isBlank()
                || initialAdminPassword.length() < 12
                || !initialAdminPhone.matches("^[0-9]{10}$")) {
            throw new IllegalStateException(
                    "Set FEASTO_INITIAL_ADMIN_EMAIL, FEASTO_INITIAL_ADMIN_PHONE, and a FEASTO_INITIAL_ADMIN_PASSWORD of at least 12 characters");
        }

        if (userRepository.existsByEmail(initialAdminEmail)) {
            return;
        }

        User superAdmin = User.builder()
                .name("Feasto Administrator")
                .email(initialAdminEmail)
                .password(passwordEncoder.encode(initialAdminPassword))
                .phone(initialAdminPhone)
                .role(Role.SUPER_ADMIN)
                .region("GLOBAL")
                .address("Feasto HQ")
                .build();
        userRepository.save(superAdmin);
    }
}
