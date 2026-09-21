package com.klef.sdp.payment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;
import com.klef.sdp.payment.entity.Payment;
import com.klef.sdp.payment.exception.PaymentNotFoundException;
import com.klef.sdp.payment.repository.PaymentRepository;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;

    public PaymentServiceImpl(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentResponse create(PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return mapToResponse(repository.save(payment));
    }

    @Override
    public PaymentResponse getById(Long paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with id: " + paymentId));
        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getByOrderId(Long orderId) {
        return repository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
