package com.klef.sdp.payment.service;

import java.util.List;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse create(PaymentRequest request);

    PaymentResponse getById(Long paymentId);

    List<PaymentResponse> getByOrderId(Long orderId);
}
