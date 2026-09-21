package com.klef.sdp.configuration;

import java.util.Optional;

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

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Optional<User> adminOpt = userRepository.findByEmail("admin@feasto.com");

        if (adminOpt.isEmpty()) {
            User superAdmin = User.builder()
                    .name("Super Admin")
                    .email("admin@feasto.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0000000000")
                    .role(Role.SUPER_ADMIN)
                    .region("GLOBAL")
                    .address("Feasto HQ")
                    .build();

            userRepository.save(superAdmin);
            System.out.println("==================================================");
            System.out.println(">>> INITIAL SUPER ADMIN CREATED SUCCESSFUL <<<");
            System.out.println(">>> Email: admin@feasto.com");
            System.out.println(">>> Password: admin123");
            System.out.println("==================================================");
        } else {
            User superAdmin = adminOpt.get();
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setPassword(passwordEncoder.encode("admin123"));
            userRepository.save(superAdmin);
            System.out.println("==================================================");
            System.out.println(">>> SUPER ADMIN UPDATED WITH PASSWORD admin123 <<<");
            System.out.println(">>> Email: admin@feasto.com");
            System.out.println(">>> Password: admin123");
            System.out.println("==================================================");
        }
    }
}
