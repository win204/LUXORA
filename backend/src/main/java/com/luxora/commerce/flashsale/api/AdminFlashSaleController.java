package com.luxora.commerce.flashsale.api;

import com.luxora.commerce.flashsale.dto.CreateFlashSaleRequest;
import com.luxora.commerce.flashsale.dto.FlashSaleResponse;
import com.luxora.commerce.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/flash-sales")
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;

    public AdminFlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @GetMapping
    public ResponseEntity<List<FlashSaleResponse>> listAll() {
        return ResponseEntity.ok(flashSaleService.getAllFlashSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlashSaleResponse> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(flashSaleService.getFlashSale(id));
    }

    @PostMapping
    public ResponseEntity<FlashSaleResponse> create(@Valid @RequestBody CreateFlashSaleRequest request) {
        FlashSaleResponse created = flashSaleService.createFlashSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FlashSaleResponse> cancel(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(flashSaleService.cancelFlashSale(id));
    }
}
