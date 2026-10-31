package com.klef.sdp.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-security-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "feasto.internal-service-token=test-internal-service-token-0123456789",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class PaymentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void paymentDataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/payment/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/payment/1")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentCreationRejectsNonPositiveOrderIds() throws Exception {
        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":0,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentCreationIsInternalAndPaymentReadsAreOwnerScoped() throws Exception {
        String customerToken = signedToken(101, "ROLE_CUSTOMER");

        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":77,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .header("Idempotency-Key", "order-77-payment-attempt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":77,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long paymentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("paymentId").asLong();

        mockMvc.perform(get("/payment/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + signedToken(202, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/payment/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payment/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + signedToken(303, "ROLE_SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void duplicatePaymentRequestReturnsTheOriginalPayment() throws Exception {
        String customerToken = signedToken(101, "ROLE_CUSTOMER");
        String idempotencyKey = "order-78-payment-attempt-1";

        MvcResult first = mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":78,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult retry = mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":78,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long firstPaymentId = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("paymentId").asLong();
        long retryPaymentId = objectMapper.readTree(retry.getResponse().getContentAsString())
                .get("paymentId").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(firstPaymentId, retryPaymentId);

        MvcResult payments = mockMvc.perform(get("/payment/order/78")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(1,
                objectMapper.readTree(payments.getResponse().getContentAsString()).size());
    }

    @Test
    void idempotencyKeyCannotBeReusedForDifferentPaymentData() throws Exception {
        String customerToken = signedToken(101, "ROLE_CUSTOMER");
        String idempotencyKey = "order-79-payment-attempt-1";

        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":79,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":79,"amount":13.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentCreationRequiresAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer " + signedToken(101, "ROLE_CUSTOMER"))
                        .header("X-Feasto-Service-Token", "test-internal-service-token-0123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":80,"amount":12.50,"paymentMethod":"CARD"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String signedToken(long userId, String role) throws Exception {
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long issuedAt = System.currentTimeMillis() / 1000;
        String claims = "{\"sub\":\"payment-test\",\"iat\":" + issuedAt
                + ",\"exp\":" + (issuedAt + 300)
                + ",\"role\":\"" + role + "\",\"userId\":" + userId + "}";
        String payload = encoder.encodeToString(claims.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                java.util.Base64.getDecoder().decode(
                        "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="),
                "HmacSHA256"));
        String signature = encoder.encodeToString(
                mac.doFinal(signingInput.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        return signingInput + "." + signature;
    }
}
