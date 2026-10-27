package com.luxora.commerce.promotion.api;

import com.luxora.commerce.promotion.dto.AvailablePromotionResponse;
import com.luxora.commerce.promotion.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailablePromotionResponse>> getAvailablePromotions(
            @RequestParam(required = false, defaultValue = "0") BigDecimal subtotal
    ) {
        return ResponseEntity.ok(promotionService.getAvailablePromotions(subtotal));
    }
}
