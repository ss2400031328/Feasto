package com.klef.sdp.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.sdp.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);
}
