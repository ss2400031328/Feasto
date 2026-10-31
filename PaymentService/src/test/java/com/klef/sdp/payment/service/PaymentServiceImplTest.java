package com.klef.sdp.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.klef.sdp.payment.dto.PaymentRequest;
import com.klef.sdp.payment.entity.Payment;
import com.klef.sdp.payment.exception.IdempotencyConflictException;
import com.klef.sdp.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void duplicateRequestReplaysExistingPayment() {
        Payment existing = existingPayment();
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.of(existing));

        var response = paymentService.create(request(), 101L, "key-1");

        assertEquals(12L, response.getPaymentId());
        verify(repository).findByIdempotencyKey("key-1");
    }

    @Test
    void concurrentDuplicateInsertReadsTheCommittedPayment() {
        Payment existing = existingPayment();
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(repository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        var response = paymentService.create(request(), 101L, "key-1");

        assertEquals(12L, response.getPaymentId());
        verify(repository, times(2)).findByIdempotencyKey("key-1");
    }

    @Test
    void reusedKeyWithDifferentRequestReturnsConflict() {
        when(repository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.of(existingPayment()));

        PaymentRequest differentRequest =
                new PaymentRequest(77L, new BigDecimal("13.50"), "CARD");

        assertThrows(IdempotencyConflictException.class,
                () -> paymentService.create(differentRequest, 101L, "key-1"));
    }

    private PaymentRequest request() {
        return new PaymentRequest(77L, new BigDecimal("12.50"), "CARD");
    }

    private Payment existingPayment() {
        return Payment.builder()
                .paymentId(12L)
                .orderId(77L)
                .userId(101L)
                .amount(new BigDecimal("12.50"))
                .paymentMethod("CARD")
                .status("SUCCESS")
                .idempotencyKey("key-1")
                .build();
    }
}
