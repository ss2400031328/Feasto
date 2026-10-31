package com.klef.sdp.payment.service;

import java.util.List;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse create(PaymentRequest request, Long userId, String idempotencyKey);

    PaymentResponse getById(Long paymentId, Long userId, boolean administrator);

    List<PaymentResponse> getByOrderId(Long orderId, Long userId, boolean administrator);
}
