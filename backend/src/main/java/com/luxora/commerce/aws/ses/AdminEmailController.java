package com.luxora.commerce.aws.ses;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/emails")
public class AdminEmailController {

    private final EmailService emailService;

    public AdminEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    public record TestEmailRequest(
            @NotBlank(message = "Recipient email is required")
            @Email(message = "Invalid email format")
            String toEmail,
            String type,
            String subject
    ) {}

    public record TestEmailResponse(
            boolean success,
            String message,
            String toEmail,
            String type
    ) {}

    @PostMapping("/test")
    public ResponseEntity<TestEmailResponse> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        String type = request.type() != null ? request.type().trim().toUpperCase() : "GENERIC";
        String recipient = request.toEmail().trim();

        switch (type) {
            case "SHIPMENT" -> emailService.sendShipmentNotification(
                    recipient,
                    "LUX-TEST-8888",
                    "GHN Express",
                    "GHN-LUX-TEST99",
                    "https://donhang.ghn.vn/?order_code=GHN-LUX-TEST99"
            );
            case "TIER_UPGRADE" -> emailService.sendTierUpgradeNotification(
                    recipient,
                    "Luxora Connoisseur",
                    "SILVER",
                    "PLATINUM",
                    15200,
                    2.0,
                    true
            );
            case "ORDER_CONFIRMATION" -> {
                String sub = request.subject() != null && !request.subject().isBlank()
                        ? request.subject()
                        : "LUXORA Haute Horlogerie - Test Order Confirmation";
                emailService.sendEmail(
                        recipient,
                        sub,
                        "<div style='font-family:sans-serif;padding:24px;background:#112623;color:#f9f9f9;border-radius:8px;'>"
                                + "<h2 style='color:#c5a059;margin-top:0;'>LUXORA Test Order Confirmation</h2>"
                                + "<p>This is a verified test dispatch from the LUXORA AWS SES Integration engine.</p>"
                                + "<p>Order Reference: <strong>#LUX-TEST-ORD01</strong></p>"
                                + "<p>Recipient: <strong>" + recipient + "</strong></p>"
                                + "</div>"
                );
            }
            default -> {
                String sub = request.subject() != null && !request.subject().isBlank()
                        ? request.subject()
                        : "LUXORA System Notification - AWS SES Test Delivery";
                String body = "<div style='font-family:sans-serif;padding:20px;color:#333;'>"
                        + "<h3>LUXORA AWS SES Test Delivery</h3>"
                        + "<p>This is a verified delivery test from LUXORA E-Commerce AWS SES Mail Engine.</p>"
                        + "<p>Recipient: <strong>" + recipient + "</strong></p>"
                        + "<p>Status: Service operational.</p>"
                        + "</div>";
                emailService.sendEmail(recipient, sub, body);
            }
        }

        return ResponseEntity.ok(new TestEmailResponse(
                true,
                "Test email dispatch triggered successfully for " + recipient + " (Template: " + type + ")",
                recipient,
                type
        ));
    }
}
