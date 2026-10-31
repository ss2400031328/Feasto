package com.klef.sdp.payment.controller;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;
import com.klef.sdp.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService service;
    private final String serviceToken;

    public PaymentController(
            PaymentService service,
            @Value("${feasto.internal-service-token}") String serviceToken) {
        if (serviceToken.length() < 32) {
            throw new IllegalArgumentException(
                    "FEASTO_INTERNAL_SERVICE_TOKEN must contain at least 32 characters");
        }
        this.service = service;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> create(
            @RequestBody @Valid PaymentRequest request,
            Authentication authentication,
            @RequestHeader(name = "X-Feasto-Service-Token", required = false)
                    String suppliedServiceToken,
            @Parameter(name = "Idempotency-Key", required = true,
                    description = "Stable key reused for retries of the same payment attempt")
            @RequestHeader(name = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        requireInternalService(suppliedServiceToken);
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1 to 128 non-blank characters");
        }
        return ResponseEntity.status(201)
                .body(service.create(request, currentUserId(authentication), idempotencyKey));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable Long paymentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.getById(
                paymentId,
                currentUserId(authentication),
                isAdministrator(authentication)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getByOrderId(
            @PathVariable Long orderId,
            Authentication authentication) {
        return ResponseEntity.ok(service.getByOrderId(
                orderId,
                currentUserId(authentication),
                isAdministrator(authentication)));
    }

    private void requireInternalService(String suppliedToken) {
        if (serviceToken.isBlank() || suppliedToken == null
                || !MessageDigest.isEqual(
                        serviceToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Payment creation is restricted to the Order Service");
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object claim = jwtAuthentication.getToken().getClaim("userId");
            if (claim instanceof Number userId) {
                return userId.longValue();
            }
        }
        throw new AccessDeniedException("Authenticated token has no user identity");
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
