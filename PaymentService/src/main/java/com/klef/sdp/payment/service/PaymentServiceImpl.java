package com.klef.sdp.payment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.dto.PaymentResponse;
import com.klef.sdp.payment.entity.Payment;
import com.klef.sdp.payment.exception.IdempotencyConflictException;
import com.klef.sdp.payment.exception.PaymentNotFoundException;
import com.klef.sdp.payment.repository.PaymentRepository;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;

    public PaymentServiceImpl(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentResponse create(
            PaymentRequest request, Long userId, String idempotencyKey) {
        Payment existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return replayOrReject(existing, request, userId);
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(userId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status("SUCCESS")
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            return mapToResponse(repository.save(payment));
        } catch (DataIntegrityViolationException exception) {
            Payment concurrentReplay = repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            return replayOrReject(concurrentReplay, request, userId);
        }
    }

    private PaymentResponse replayOrReject(
            Payment existing, PaymentRequest request, Long userId) {
        if (!Objects.equals(existing.getOrderId(), request.getOrderId())
                || !Objects.equals(existing.getUserId(), userId)
                || existing.getAmount() == null
                || existing.getAmount().compareTo(request.getAmount()) != 0
                || !Objects.equals(existing.getPaymentMethod(), request.getPaymentMethod())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used with a different payment request");
        }
        return mapToResponse(existing);
    }

    @Override
    public PaymentResponse getById(Long paymentId, Long userId, boolean administrator) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with id: " + paymentId));
        if (!administrator && !userId.equals(payment.getUserId())) {
            throw new AccessDeniedException("You can only access your own payments");
        }
        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getByOrderId(Long orderId, Long userId, boolean administrator) {
        List<Payment> payments = administrator
                ? repository.findByOrderId(orderId)
                : repository.findByOrderIdAndUserId(orderId, userId);
        return payments.stream()
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
