package com.luxora.commerce.payment.service;

import com.luxora.commerce.payment.dto.PaymentResponse;
import com.luxora.commerce.payment.model.Payment;
import org.springframework.stereotype.Component;

@Component
class PaymentMapper {

    PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getProvider(),
                payment.getProviderReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}