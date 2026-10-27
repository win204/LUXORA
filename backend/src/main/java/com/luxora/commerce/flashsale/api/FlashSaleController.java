package com.luxora.commerce.flashsale.api;

import com.luxora.commerce.flashsale.dto.FlashSaleResponse;
import com.luxora.commerce.flashsale.service.FlashSaleService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flash-sales")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @GetMapping("/active")
    public ResponseEntity<FlashSaleResponse> getActiveFlashSale() {
        return flashSaleService.getActiveFlashSale()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlashSaleResponse> getFlashSaleById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(flashSaleService.getFlashSale(id));
    }
}
