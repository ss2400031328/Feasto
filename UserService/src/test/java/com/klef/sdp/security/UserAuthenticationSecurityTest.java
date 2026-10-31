package com.klef.sdp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klef.sdp.entity.Role;
import com.klef.sdp.entity.User;
import com.klef.sdp.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-security-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "jwt.expiration=3600000",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class UserAuthenticationSecurityTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final AtomicInteger PHONE_COUNTER = new AtomicInteger(1000000000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationHashesPasswordAndAlwaysCreatesCustomer() throws Exception {
        Account account = newAccount();
        JsonNode response = register(account, "SUPER_ADMIN");

        org.junit.jupiter.api.Assertions.assertEquals("CUSTOMER", response.get("role").asText());
        User storedUser = userRepository.findByEmail(account.email()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(account.password(), storedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches(account.password(), storedUser.getPassword()));
    }

    @Test
    void loginIssuesUsableJwtAndProtectedProfileRequiresIt() throws Exception {
        Account account = newAccount();
        register(account, null);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());

        String token = login(account);
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.email()));
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        Account account = newAccount();
        JsonNode registered = register(account, null);
        long userId = registered.get("id").asLong();
        String expiredToken = Jwts.builder()
                .subject(account.email())
                .claim("userId", userId)
                .claim("role", "ROLE_CUSTOMER")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() - 120_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReadAnotherProfileOrCreateAnAdmin() throws Exception {
        Account customer = newAccount();
        register(customer, null);
        Account other = newAccount();
        JsonNode otherProfile = register(other, null);
        String token = login(customer);

        mockMvc.perform(get("/users/{id}", otherProfile.get("id").asLong())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/users/admin/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(newAccount(), "REGIONAL_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRejectsBlankFieldsInvalidEmailAndShortPassword() throws Exception {
        Account account = newAccount();
        JsonNode profile = register(account, null);
        String token = login(account);
        long userId = profile.get("id").asLong();

        for (String invalidUpdate : new String[] {
                "{\"name\":\"   \"}",
                "{\"email\":\"not-an-email\"}",
                "{\"email\":\"   \"}",
                "{\"password\":\"short\"}",
                "{\"phone\":\" \"}",
                "{\"address\":\" \"}",
                "{\"region\":\" \"}"
        }) {
            mockMvc.perform(put("/users/{id}", userId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidUpdate))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void superAdminCanCreateRegionalManager() throws Exception {
        Account administrator = newAccount();
        userRepository.save(User.builder()
                .name("Test Admin")
                .email(administrator.email())
                .password(passwordEncoder.encode(administrator.password()))
                .phone(administrator.phone())
                .address("Test address")
                .role(Role.SUPER_ADMIN)
                .region("GLOBAL")
                .build());
        String token = login(administrator);

        mockMvc.perform(post("/users/admin/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(newAccount(), "REGIONAL_MANAGER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("REGIONAL_MANAGER"));
    }

    private JsonNode register(Account account, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(account, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(Account account) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "email", account.email(),
                                        "password", account.password()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String registrationJson(Account account, String role) throws Exception {
        var request = new java.util.LinkedHashMap<String, Object>();
        request.put("name", "Test User");
        request.put("email", account.email());
        request.put("password", account.password());
        request.put("phone", account.phone());
        request.put("address", "Test address");
        if (role != null) {
            request.put("role", role);
        }
        return objectMapper.writeValueAsString(request);
    }

    private Account newAccount() {
        return new Account(
                "test-" + UUID.randomUUID() + "@example.test",
                "test-password-123",
                String.format("%010d", PHONE_COUNTER.incrementAndGet()));
    }

    private record Account(String email, String password, String phone) {
    }
}
