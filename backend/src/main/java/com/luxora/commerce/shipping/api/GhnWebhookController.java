package com.luxora.commerce.shipping.api;

import com.luxora.commerce.order.shipment.service.ShipmentService;
import com.luxora.commerce.shipping.dto.GhnWebhookPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipping Webhooks", description = "Endpoints for carrier webhook notifications")
public class GhnWebhookController {

    private final ShipmentService shipmentService;

    public GhnWebhookController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/ghn-webhook")
    @Operation(summary = "Handle GHN shipping status webhook", description = "Receives tracking status updates from GHN Open API")
    public ResponseEntity<Map<String, Object>> handleGhnWebhook(
            @RequestBody GhnWebhookPayload payload,
            @RequestHeader(value = "X-GHN-Token", required = false) String tokenHeader,
            @RequestHeader(value = "Token", required = false) String altTokenHeader) {
        String token = tokenHeader != null ? tokenHeader : altTokenHeader;
        shipmentService.processGhnWebhook(payload, token);
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Webhook processed"));
    }
}
