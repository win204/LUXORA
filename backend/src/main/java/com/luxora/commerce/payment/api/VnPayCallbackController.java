package com.luxora.commerce.payment.api;

import com.luxora.commerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
public class VnPayCallbackController {

    private final PaymentService paymentService;

    public VnPayCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/vnpay-ipn")
    @Operation(summary = "VNPay IPN Webhook", description = "Server-to-server webhook endpoint called by VNPay to confirm transaction status.")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> allParams) {
        Map<String, String> response = paymentService.processVnPayIpn(allParams);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    @Operation(summary = "VNPay Return Callback", description = "Verifies return callback parameters and signature after user redirected back from VNPay.")
    public ResponseEntity<Map<String, Object>> vnpayReturn(@RequestParam Map<String, String> allParams) {
        Map<String, Object> response = paymentService.verifyVnPayReturn(allParams);
        return ResponseEntity.ok(response);
    }
}
