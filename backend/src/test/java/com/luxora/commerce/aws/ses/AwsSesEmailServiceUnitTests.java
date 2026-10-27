package com.luxora.commerce.aws.ses;

import com.luxora.commerce.aws.config.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AwsSesEmailServiceUnitTests {

    @Mock
    private SesClient sesClient;

    private AwsProperties properties;
    private AwsSesEmailService emailService;

    @BeforeEach
    void setUp() {
        properties = new AwsProperties();
        properties.getSes().setFromEmail("noreply@luxora.com");
        emailService = new AwsSesEmailService(sesClient, properties);
    }

    @Test
    void sendEmailCallsSesClient() {
        emailService.sendEmail("customer@example.com", "Test Subject", "<p>Hello</p>");
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendOrderConfirmationConstructsAndSendsEmail() {
        emailService.sendOrderConfirmation("customer@example.com", "12345678-abcd", "999.00", "USD");
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendShipmentNotificationConstructsAndSendsEmail() {
        emailService.sendShipmentNotification(
                "customer@example.com",
                "12345678-abcd",
                "GHN",
                "GHN123456",
                "https://donhang.ghn.vn/?order_code=GHN123456"
        );
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendTierUpgradeNotificationConstructsAndSendsEmail() {
        emailService.sendTierUpgradeNotification(
                "vip@example.com",
                "Alexander Vance",
                "SILVER",
                "PLATINUM",
                12500,
                2.0,
                true
        );
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendEmailSuppressesExceptionsGracefully() {
        org.mockito.Mockito.doThrow(new RuntimeException("SES offline or credentials rejected"))
                .when(sesClient).sendEmail(any(SendEmailRequest.class));

        // Must not throw exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                emailService.sendEmail("recipient@example.com", "Subject", "<p>Test</p>")
        );
    }
}
