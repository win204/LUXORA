package com.luxora.commerce.checkout.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.checkout.dto.CheckoutPreviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutPreviewService {

    private final CheckoutCalculationService checkoutCalculationService;

    public CheckoutPreviewService(CheckoutCalculationService checkoutCalculationService) {
        this.checkoutCalculationService = checkoutCalculationService;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(AuthenticatedUser user, CheckoutPreviewRequest request) {
        return checkoutCalculationService.calculate(user.id(), request, false).toPreviewResponse();
    }
}