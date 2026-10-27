package com.luxora.commerce.order.model;

import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.order.shipment.model.Shipment;
import com.luxora.commerce.promotion.service.PromotionService;
import com.luxora.commerce.user.model.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Column(name = "guest_access_token", length = 64)
    private String guestAccessToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "recipient_name", nullable = false, length = 160)
    private String recipientName;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "address_line1", nullable = false, length = 240)
    private String addressLine1;

    @Column(name = "address_line2", length = 240)
    private String addressLine2;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 120)
    private String province;

    @Column(nullable = false, length = 120)
    private String country;

    @Column(name = "postal_code", length = 24)
    private String postalCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "promotion_code", length = 64)
    private String promotionCode;

    @Column(name = "promotion_name", length = 160)
    private String promotionName;

    @Column(name = "promotion_type", length = 32)
    private String promotionType;

    @Column(name = "promotion_value", precision = 12, scale = 2)
    private BigDecimal promotionValue;

    @Column(name = "promotion_discount_amount", precision = 12, scale = 2)
    private BigDecimal promotionDiscountAmount;

    @Column(name = "loyalty_points_redeemed")
    private Integer loyaltyPointsRedeemed = 0;

    @Column(name = "loyalty_discount", precision = 12, scale = 2)
    private BigDecimal loyaltyDiscount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Shipment shipment;

    protected Order() {}

    public Order(
            User user,
            String customerEmail,
            String guestAccessToken,
            ShippingAddressResponse a,
            BigDecimal sub,
            BigDecimal ship,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal total,
            String currency,
            PromotionService.AppliedPromotion promotion) {
        this.user = user;
        this.customerEmail = customerEmail;
        this.guestAccessToken = guestAccessToken;
        this.recipientName = a.recipientName();
        this.phone = a.phone();
        this.addressLine1 = a.addressLine1();
        this.addressLine2 = a.addressLine2();
        this.city = a.city();
        this.province = a.province();
        this.country = a.country();
        this.postalCode = a.postalCode();
        this.subtotal = sub;
        this.shippingFee = ship;
        this.tax = tax;
        this.discount = discount;
        this.grandTotal = total;
        this.currency = currency;
        if (promotion != null && promotion.promotion() != null) {
            this.promotionCode = promotion.promotion().getCode();
            this.promotionName = promotion.promotion().getName();
            this.promotionType = promotion.promotion().getType().name();
            this.promotionValue = promotion.promotion().getValue();
            this.promotionDiscountAmount = promotion.discountAmount();
        }
    }

    public Order(
            User u,
            ShippingAddressResponse a,
            BigDecimal sub,
            BigDecimal ship,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal total,
            String currency,
            PromotionService.AppliedPromotion promotion) {
        this(u, u != null ? u.getEmail() : null, null, a, sub, ship, tax, discount, total, currency, promotion);
    }

    @PrePersist
    void pre() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void upd() {
        updatedAt = Instant.now();
    }

    public void addItem(OrderItem i) {
        i.assignOrder(this);
        items.add(i);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public boolean isGuestOrder() { return user == null; }
    public String getCustomerEmail() { return customerEmail; }
    public String getGuestAccessToken() { return guestAccessToken; }
    public OrderStatus getStatus() { return status; }
    public void markPaid() { status = OrderStatus.PAID; }
    public void markCancelled() { status = OrderStatus.CANCELLED; }
    public void changeStatus(OrderStatus s) { status = s; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getProvince() { return province; }
    public String getCountry() { return country; }
    public String getPostalCode() { return postalCode; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAdminNote() { return adminNote; }
    public void updateAdminNote(String n) { adminNote = n; }
    public List<OrderItem> getItems() { return items; }
    public Shipment getShipment() { return shipment; }
    public void attachShipment(Shipment s) { shipment = s; }
    public String getPromotionCode() { return promotionCode; }
    public String getPromotionName() { return promotionName; }
    public String getPromotionType() { return promotionType; }
    public BigDecimal getPromotionValue() { return promotionValue; }
    public BigDecimal getPromotionDiscountAmount() { return promotionDiscountAmount; }
    public int getLoyaltyPointsRedeemed() { return loyaltyPointsRedeemed != null ? loyaltyPointsRedeemed : 0; }
    public void setLoyaltyPointsRedeemed(Integer loyaltyPointsRedeemed) { this.loyaltyPointsRedeemed = loyaltyPointsRedeemed != null ? loyaltyPointsRedeemed : 0; }
    public BigDecimal getLoyaltyDiscount() { return loyaltyDiscount != null ? loyaltyDiscount : BigDecimal.ZERO; }
    public void setLoyaltyDiscount(BigDecimal loyaltyDiscount) { this.loyaltyDiscount = loyaltyDiscount != null ? loyaltyDiscount : BigDecimal.ZERO; }
}