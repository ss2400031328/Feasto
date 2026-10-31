package com.klef.sdp.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.klef.sdp.orderservice.dto.PaymentRequest;
import com.klef.sdp.orderservice.dto.PaymentResponse;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/payment/create")
    PaymentResponse createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request);
}
