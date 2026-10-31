package com.klef.sdp.gateway.security;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "eureka.client.enabled=false"
})
class GatewaySecurityTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Value("${local.server.port}")
    private int port;

    @Test
    void protectedRoutesRejectMissingInvalidAndSpoofedIdentityHeaders() {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        webTestClient.get()
                .uri("/orders/")
                .header("X-User-Role", "ROLE_SUPER_ADMIN")
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri("/orders/")
                .headers(headers -> headers.setBearerAuth("not-a-valid-jwt"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validSignedJwtPassesGatewayAuthentication() throws Exception {
        WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        String token = signedCustomerToken();

        webTestClient.get()
                .uri("/orders/")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    private String signedCustomerToken() throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        long issuedAt = System.currentTimeMillis() / 1000;
        String claims = "{\"sub\":\"gateway-test\",\"iat\":" + issuedAt
                + ",\"exp\":" + (issuedAt + 300)
                + ",\"role\":\"ROLE_CUSTOMER\",\"userId\":1}";
        String payload = encoder.encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(TEST_SECRET), "HmacSHA256"));
        String signature = encoder.encodeToString(
                mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
        return signingInput + "." + signature;
    }
}
