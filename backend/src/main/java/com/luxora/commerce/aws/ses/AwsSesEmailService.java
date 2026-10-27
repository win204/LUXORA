package com.luxora.commerce.aws.ses;

import com.luxora.commerce.aws.config.AwsProperties;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
public class AwsSesEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(AwsSesEmailService.class);

    private final SesClient sesClient;
    private final AwsProperties properties;

    public AwsSesEmailService(SesClient sesClient, AwsProperties properties) {
        this.sesClient = sesClient;
        this.properties = properties;
    }

    @Override
    public void sendEmail(String toEmail, String subject, String htmlBody) {
        String fromEmail = properties.getSes().getFromEmail();
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("Sent email via AWS SES to '{}' with subject: '{}'", toEmail, subject);
        } catch (Exception e) {
            log.warn("Failed to send email via AWS SES to '{}': {}. (Running in dev/sandbox mode)", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendOrderConfirmation(String toEmail, String orderId, String grandTotal, String currency) {
        String shortId = orderId.length() >= 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        String subject = "LUXORA - Order Confirmation #" + shortId;
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"/></head>
                <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0;">
                        <div style="background-color: #112623; padding: 28px 24px; text-align: center; border-bottom: 2px solid #c5a059;">
                            <h1 style="color: #e6c887; margin: 0; font-size: 24px; letter-spacing: 0.15em;">LUXORA</h1>
                            <p style="color: rgba(255,255,255,0.7); margin: 6px 0 0 0; font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase;">Haute Horlogerie & Technology</p>
                        </div>
                        <div style="padding: 28px 24px;">
                            <h2 style="color: #111827; margin: 0 0 12px 0; font-size: 20px;">Order Confirmation</h2>
                            <p style="color: #4b5563; font-size: 15px; line-height: 1.6; margin: 0 0 20px 0;">
                                Thank you for your distinguished patronage. Your order <strong>#%s</strong> has been confirmed.
                            </p>
                            <div style="background-color: #f8fafc; padding: 18px; border-radius: 8px; border: 1px solid #e2e8f0; margin-bottom: 24px;">
                                <p style="margin: 0; color: #4b5563; font-size: 13px; text-transform: uppercase; letter-spacing: 0.05em;">Total Amount Paid</p>
                                <p style="margin: 4px 0 0 0; color: #112623; font-size: 24px; font-weight: 800;">$%s %s</p>
                            </div>
                            <p style="color: #6b7280; font-size: 13px; line-height: 1.5; margin: 0;">
                                You will receive another notification with a courier tracking code once your package is dispatched.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(shortId, grandTotal, currency);

        sendEmail(toEmail, subject, htmlBody);
    }

    @Override
    public void sendOrderConfirmation(Order order) {
        String shortId = order.getId().toString().substring(0, 8).toUpperCase();
        String subject = "LUXORA - Order Confirmation & Invoice #" + shortId;

        StringBuilder itemsTable = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String variantInfo = item.getVariantName() != null ? item.getVariantName() : (item.getColor() != null ? item.getColor() : "");
            itemsTable.append("""
                    <tr style="border-bottom: 1px solid #f1f5f9;">
                        <td style="padding: 12px 8px; color: #1e293b; font-size: 14px;">
                            <strong>%s</strong>
                            <div style="color: #64748b; font-size: 12px;">%s (SKU: %s)</div>
                        </td>
                        <td style="padding: 12px 8px; text-align: center; color: #475569; font-size: 14px;">%d</td>
                        <td style="padding: 12px 8px; text-align: right; color: #475569; font-size: 14px;">$%s</td>
                        <td style="padding: 12px 8px; text-align: right; color: #1e293b; font-weight: 700; font-size: 14px;">$%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProductName()),
                    escapeHtml(variantInfo),
                    escapeHtml(item.getSku()),
                    item.getQuantity(),
                    item.getUnitPrice().setScale(2),
                    item.getLineTotal().setScale(2)
            ));
        }

        StringBuilder discountRows = new StringBuilder();
        if (order.getPromotionDiscountAmount() != null && order.getPromotionDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountRows.append("""
                    <tr>
                        <td style="padding: 6px 0; color: #64748b; font-size: 13px;">Promotion Discount (%s):</td>
                        <td style="padding: 6px 0; text-align: right; color: #166534; font-weight: 600; font-size: 13px;">-$%s</td>
                    </tr>
                    """.formatted(escapeHtml(order.getPromotionCode()), order.getPromotionDiscountAmount().setScale(2)));
        }
        if (order.getLoyaltyDiscount() != null && order.getLoyaltyDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountRows.append("""
                    <tr>
                        <td style="padding: 6px 0; color: #64748b; font-size: 13px;">Loyalty Rewards (%d PTS):</td>
                        <td style="padding: 6px 0; text-align: right; color: #166534; font-weight: 600; font-size: 13px;">-$%s</td>
                    </tr>
                    """.formatted(order.getLoyaltyPointsRedeemed(), order.getLoyaltyDiscount().setScale(2)));
        }

        String addressBlock = "%s<br/>%s<br/>%s, %s %s<br/>%s<br/>Phone: %s".formatted(
                escapeHtml(order.getRecipientName()),
                escapeHtml(order.getAddressLine1() + (order.getAddressLine2() != null ? " " + order.getAddressLine2() : "")),
                escapeHtml(order.getCity()),
                escapeHtml(order.getProvince()),
                escapeHtml(order.getPostalCode() != null ? order.getPostalCode() : ""),
                escapeHtml(order.getCountry()),
                escapeHtml(order.getPhone())
        );

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"/></head>
                <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <div style="max-width: 640px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 16px rgba(0,0,0,0.06);">
                        <div style="background-color: #112623; padding: 32px 28px; text-align: center; border-bottom: 2px solid #c5a059;">
                            <h1 style="color: #e6c887; margin: 0; font-size: 26px; letter-spacing: 0.15em;">LUXORA</h1>
                            <p style="color: rgba(255,255,255,0.7); margin: 6px 0 0 0; font-size: 11px; letter-spacing: 0.12em; text-transform: uppercase;">Official Purchase Receipt & Tax Invoice</p>
                        </div>
                        <div style="padding: 32px 28px;">
                            <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20px; border-bottom: 1px solid #f1f5f9; padding-bottom: 12px;">
                                <div>
                                    <h2 style="color: #111827; margin: 0; font-size: 20px;">Order Confirmation</h2>
                                    <p style="color: #64748b; margin: 4px 0 0 0; font-size: 13px;">Order Reference: <strong>#%s</strong></p>
                                </div>
                            </div>
                            <p style="color: #4b5563; font-size: 14px; line-height: 1.6; margin: 0 0 24px 0;">
                                Dear <strong>%s</strong>,<br/>
                                Thank you for choosing LUXORA. We have received your order and our specialists are preparing it for delivery.
                            </p>

                            <!-- Items Table -->
                            <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px;">
                                <thead>
                                    <tr style="border-bottom: 2px solid #e2e8f0; text-align: left;">
                                        <th style="padding: 8px; color: #475569; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;">Item</th>
                                        <th style="padding: 8px; text-align: center; color: #475569; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;">Qty</th>
                                        <th style="padding: 8px; text-align: right; color: #475569; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;">Unit</th>
                                        <th style="padding: 8px; text-align: right; color: #475569; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;">Total</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    %s
                                </tbody>
                            </table>

                            <!-- Financial Totals -->
                            <div style="background-color: #f8fafc; padding: 20px; border-radius: 8px; border: 1px solid #e2e8f0; margin-bottom: 24px;">
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 6px 0; color: #64748b; font-size: 13px;">Subtotal:</td>
                                        <td style="padding: 6px 0; text-align: right; color: #1e293b; font-weight: 600; font-size: 13px;">$%s</td>
                                    </tr>
                                    %s
                                    <tr>
                                        <td style="padding: 6px 0; color: #64748b; font-size: 13px;">Shipping Fee:</td>
                                        <td style="padding: 6px 0; text-align: right; color: #1e293b; font-weight: 600; font-size: 13px;">$%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #64748b; font-size: 13px;">Estimated Tax:</td>
                                        <td style="padding: 6px 0; text-align: right; color: #1e293b; font-weight: 600; font-size: 13px;">$%s</td>
                                    </tr>
                                    <tr style="border-top: 2px solid #cbd5e1;">
                                        <td style="padding: 10px 0 0 0; color: #112623; font-weight: 800; font-size: 16px;">Grand Total:</td>
                                        <td style="padding: 10px 0 0 0; text-align: right; color: #112623; font-weight: 900; font-size: 20px;">$%s %s</td>
                                    </tr>
                                </table>
                            </div>

                            <!-- Shipping Destination -->
                            <div style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin-bottom: 24px;">
                                <p style="margin: 0 0 8px 0; color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.08em;">Delivery Address</p>
                                <p style="margin: 0; color: #334155; font-size: 14px; line-height: 1.5;">%s</p>
                            </div>

                            <p style="color: #94a3b8; font-size: 12px; line-height: 1.5; margin: 0; text-align: center;">
                                LUXORA Concierge Service &bull; For inquiries, reply directly to this message.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                shortId,
                escapeHtml(order.getRecipientName()),
                itemsTable.toString(),
                order.getSubtotal().setScale(2),
                discountRows.toString(),
                order.getShippingFee().setScale(2),
                order.getTax().setScale(2),
                order.getGrandTotal().setScale(2),
                escapeHtml(order.getCurrency()),
                addressBlock
        );

        sendEmail(order.getCustomerEmail(), subject, htmlBody);
    }

    @Override
    public void sendShipmentNotification(String toEmail, String orderId, String carrier, String trackingNumber, String trackingUrl) {
        String shortId = orderId.length() >= 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        String subject = "LUXORA - Your Order #" + shortId + " is on its way";

        String trackingBtn = (trackingUrl != null && !trackingUrl.isBlank()) ? """
                <div style="margin-top: 20px; text-align: center;">
                    <a href="%s" style="display: inline-block; background-color: #112623; color: #e6c887; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 14px; border: 1px solid #c5a059;">
                        Track Shipment Live &rarr;
                    </a>
                </div>
                """.formatted(escapeHtml(trackingUrl)) : "";

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"/></head>
                <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 16px rgba(0,0,0,0.06);">
                        <div style="background-color: #112623; padding: 28px 24px; text-align: center; border-bottom: 2px solid #c5a059;">
                            <h1 style="color: #e6c887; margin: 0; font-size: 24px; letter-spacing: 0.15em;">LUXORA</h1>
                            <p style="color: rgba(255,255,255,0.7); margin: 6px 0 0 0; font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase;">Shipment Dispatch Notice</p>
                        </div>
                        <div style="padding: 28px 24px;">
                            <h2 style="color: #111827; margin: 0 0 12px 0; font-size: 20px;">Your package is in transit</h2>
                            <p style="color: #4b5563; font-size: 15px; line-height: 1.6; margin: 0 0 20px 0;">
                                Order <strong>#%s</strong> has been carefully packed and handed over to our premier carrier partner <strong>%s</strong>.
                            </p>
                            <div style="background-color: #fdfaf3; padding: 18px; border-radius: 8px; border: 1px solid #f3deb3; margin-bottom: 20px; text-align: center;">
                                <p style="margin: 0; color: #a9792b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em;">Waybill Tracking Number</p>
                                <p style="margin: 6px 0 0 0; color: #112623; font-size: 20px; font-weight: 900; letter-spacing: 0.08em; font-family: monospace;">%s</p>
                            </div>
                            %s
                            <p style="color: #94a3b8; font-size: 12px; line-height: 1.5; margin: 24px 0 0 0; text-align: center;">
                                Expected delivery: 2-4 business days. For concierge assistance, reply directly to this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(shortId, escapeHtml(carrier), escapeHtml(trackingNumber), trackingBtn);

        sendEmail(toEmail, subject, htmlBody);
    }

    @Override
    public void sendTierUpgradeNotification(
            String toEmail,
            String customerName,
            String oldTier,
            String newTier,
            int currentPoints,
            double pointMultiplier,
            boolean freeShipping
    ) {
        String subject = "LUXORA - Welcome to " + newTier + " Privilege Status";
        String shippingPerk = freeShipping ? "Complimentary Standard Delivery on all orders" : "Priority Dispatch";

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"/></head>
                <body style="margin: 0; padding: 20px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 16px rgba(0,0,0,0.06);">
                        <div style="background-color: #112623; padding: 32px 24px; text-align: center; border-bottom: 2px solid #c5a059;">
                            <h1 style="color: #e6c887; margin: 0; font-size: 26px; letter-spacing: 0.15em;">LUXORA</h1>
                            <p style="color: rgba(255,255,255,0.7); margin: 6px 0 0 0; font-size: 11px; letter-spacing: 0.12em; text-transform: uppercase;">Membership Ascension</p>
                        </div>
                        <div style="padding: 32px 28px;">
                            <div style="text-align: center; margin-bottom: 24px;">
                                <span style="background: rgba(169, 121, 43, 0.15); color: #a9792b; border: 1px solid #c5a059; padding: 4px 12px; border-radius: 999px; font-size: 11px; font-weight: 800; letter-spacing: 0.1em;">
                                    TIER UPGRADE
                                </span>
                                <h2 style="color: #111827; margin: 12px 0 4px 0; font-size: 22px;">Congratulations, %s</h2>
                                <p style="color: #4b5563; font-size: 15px; margin: 0;">
                                    You have ascended from <strong>%s</strong> to <strong>%s</strong> Privilege.
                                </p>
                            </div>

                            <div style="background: linear-gradient(135deg, #142e2b 0%%, #0d1e1c 100%%); border-radius: 12px; padding: 24px; color: #ffffff; margin-bottom: 24px; border: 1px solid rgba(230, 200, 135, 0.3);">
                                <p style="color: rgba(255,255,255,0.65); font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase; margin: 0;">Your New Privileges</p>
                                <ul style="margin: 12px 0 0 0; padding-left: 20px; font-size: 14px; line-height: 1.8; color: #f1f5f9;">
                                    <li><strong>%.2fx Points Earning Multiplier</strong> on all purchases</li>
                                    <li><strong>%s</strong></li>
                                    <li>100 Points = $1.00 USD checkout redemption</li>
                                </ul>
                                <div style="margin-top: 16px; padding-top: 14px; border-top: 1px solid rgba(255,255,255,0.15); display: flex; justify-content: space-between;">
                                    <span style="font-size: 13px; color: rgba(255,255,255,0.8);">Current Points Balance:</span>
                                    <strong style="font-size: 15px; color: #e6c887;">%d PTS</strong>
                                </div>
                            </div>

                            <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 0; text-align: center;">
                                Thank you for being a valued part of the LUXORA circle.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(customerName),
                escapeHtml(oldTier),
                escapeHtml(newTier),
                pointMultiplier,
                escapeHtml(shippingPerk),
                currentPoints
        );

        sendEmail(toEmail, subject, htmlBody);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
