package com.luxora.commerce.loyalty.api;

import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.loyalty.dto.AdminLoyaltyAdjustRequest;
import com.luxora.commerce.loyalty.dto.LoyaltyAccountResponse;
import com.luxora.commerce.loyalty.dto.LoyaltyTransactionResponse;
import com.luxora.commerce.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/loyalty")
public class AdminLoyaltyController {

    private final LoyaltyService loyaltyService;

    public AdminLoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/users/{userId}")
    public LoyaltyAccountResponse getAccountByUserId(@PathVariable UUID userId) {
        return loyaltyService.getOrCreateAccount(userId);
    }

    @GetMapping("/users/{userId}/transactions")
    public PageResponse<LoyaltyTransactionResponse> getTransactionsByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoyaltyTransactionResponse> p = loyaltyService.getTransactions(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @PostMapping("/users/{userId}/adjust")
    public LoyaltyAccountResponse adjustPoints(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminLoyaltyAdjustRequest request) {
        return loyaltyService.adjustPoints(userId, request.points(), request.reason());
    }
}
