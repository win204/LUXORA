package com.luxora.commerce.loyalty.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.loyalty.dto.LoyaltyAccountResponse;
import com.luxora.commerce.loyalty.dto.LoyaltyTransactionResponse;
import com.luxora.commerce.loyalty.service.LoyaltyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/me")
    public LoyaltyAccountResponse getMyLoyaltyAccount(@AuthenticationPrincipal AuthenticatedUser user) {
        return loyaltyService.getOrCreateAccount(user.id());
    }

    @GetMapping("/transactions")
    public PageResponse<LoyaltyTransactionResponse> getMyTransactions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoyaltyTransactionResponse> p = loyaltyService.getTransactions(
                user.id(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
