package com.klef.sdp.payment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;
import com.klef.sdp.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> create(
            @RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(service.getById(paymentId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getByOrderId(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(service.getByOrderId(orderId));
    }
}
