package com.luxora.commerce.aws.ses;

import com.luxora.commerce.order.model.Order;

public interface EmailService {

    void sendEmail(String toEmail, String subject, String htmlBody);

    void sendOrderConfirmation(String toEmail, String orderId, String grandTotal, String currency);

    void sendOrderConfirmation(Order order);

    void sendShipmentNotification(String toEmail, String orderId, String carrier, String trackingNumber, String trackingUrl);

    void sendTierUpgradeNotification(String toEmail, String customerName, String oldTier, String newTier, int currentPoints, double pointMultiplier, boolean freeShipping);
}
