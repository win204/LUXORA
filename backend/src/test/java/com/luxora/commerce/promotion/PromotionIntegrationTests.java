package com.luxora.commerce.promotion;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.checkout.service.CheckoutCalculation;
import com.luxora.commerce.checkout.service.CheckoutCalculationService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.order.dto.OrderResponse;
import com.luxora.commerce.order.service.OrderService;
import com.luxora.commerce.payment.dto.CreatePaymentRequest;
import com.luxora.commerce.payment.dto.PaymentResponse;
import com.luxora.commerce.payment.service.PaymentService;
import com.luxora.commerce.promotion.dto.PromotionResponse;
import com.luxora.commerce.promotion.dto.PromotionUpsertRequest;
import com.luxora.commerce.promotion.model.PromotionType;
import com.luxora.commerce.promotion.repository.PromotionRepository;
import com.luxora.commerce.promotion.service.PromotionService;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("local")
class PromotionIntegrationTests {
    @Autowired private PromotionService promotions;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private CheckoutCalculationService checkout;
    @Autowired private OrderService orders;
    @Autowired private PaymentService payments;
    @Autowired private UserRepository users;
    @Autowired private ProductVariantRepository variants;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private CartStore cartStore;

    @BeforeEach
    void resetState() {
        reset(cartStore);
        jdbc.update("delete from payments"); jdbc.update("delete from order_items"); jdbc.update("delete from orders"); jdbc.update("delete from promotions");
        jdbc.update("update product_variants set active = true");
        jdbc.update("update inventory_items set quantity_available = 40");
    }

    @Test void percentageFixedCapAndCodeNormalizationAreAuthoritative() {
        PromotionResponse percent = create(" save10 ", PromotionType.PERCENTAGE, "10.00", null, null, null, true, -60, 60);
        assertThat(percent.code()).isEqualTo("SAVE10");
        assertThat(promotions.apply("save10", new BigDecimal("200.00"), false).discountAmount()).isEqualByComparingTo("20.00");
        create("FIXED", PromotionType.FIXED_AMOUNT, "25.00", null, null, null, true, -60, 60);
        assertThat(promotions.apply("fixed", new BigDecimal("20.00"), false).discountAmount()).isEqualByComparingTo("20.00");
        create("CAPPED", PromotionType.PERCENTAGE, "50.00", null, "30.00", null, true, -60, 60);
        assertThat(promotions.apply("CAPPED", new BigDecimal("100.00"), false).discountAmount()).isEqualByComparingTo("30.00");
    }

    @Test void promotionEligibilityAndValidationErrorsAreEnforced() {
        create("MIN", PromotionType.FIXED_AMOUNT, "10.00", "1000.00", null, null, true, -60, 60);
        assertCode("PROMOTION_MINIMUM_NOT_MET", () -> promotions.apply("MIN", BigDecimal.TEN, false));
        create("OFF", PromotionType.FIXED_AMOUNT, "10.00", null, null, null, false, -60, 60);
        assertCode("PROMOTION_INACTIVE", () -> promotions.apply("OFF", BigDecimal.TEN, false));
        create("FUTURE", PromotionType.FIXED_AMOUNT, "10.00", null, null, null, true, 60, 120);
        assertCode("PROMOTION_NOT_STARTED", () -> promotions.apply("FUTURE", BigDecimal.TEN, false));
        create("OLD", PromotionType.FIXED_AMOUNT, "10.00", null, null, null, true, -120, -60);
        assertCode("PROMOTION_EXPIRED", () -> promotions.apply("OLD", BigDecimal.TEN, false));
        assertThatThrownBy(() -> create("TOO-MUCH", PromotionType.PERCENTAGE, "101.00", null, null, null, true, -60, 60)).isInstanceOf(BadRequestException.class).extracting(error -> ((BadRequestException) error).code()).isEqualTo("PROMOTION_VALUE_INVALID");
        assertThatThrownBy(() -> create("ZERO", PromotionType.FIXED_AMOUNT, "0.00", null, null, null, true, -60, 60)).isInstanceOf(BadRequestException.class).extracting(error -> ((BadRequestException) error).code()).isEqualTo("PROMOTION_VALUE_INVALID");
        PromotionResponse saved = create("DUP", PromotionType.FIXED_AMOUNT, "1.00", null, null, null, true, -60, 60);
        assertThatThrownBy(() -> promotions.create(request("dup", PromotionType.FIXED_AMOUNT, "1.00", null, null, null, true, -60, 60))).isInstanceOf(ConflictException.class);
        promotionRepository.findById(saved.id()).orElseThrow().incrementUsage();
    }

    @Test void checkoutOrderPaymentAndSnapshotUseCurrentAuthoritativePromotion() {
        ProductVariant variant = variant();
        AuthenticatedUser user = user("promotion-order@example.com");
        when(cartStore.getQuantities("user:" + user.id())).thenReturn(Map.of(variant.getId(), 1));
        create("ORDER10", PromotionType.PERCENTAGE, "10.00", null, null, 2, true, -60, 60);
        CheckoutCalculation preview = checkout.calculate(user.id(), address("ORDER10"), false);
        CheckoutCalculation withoutPromotion = checkout.calculate(user.id(), address(null), false);
        BigDecimal expectedDiscount = variant.getPrice().multiply(new BigDecimal("0.10"));
        assertThat(preview.discount()).isEqualByComparingTo(expectedDiscount);
        assertThat(withoutPromotion.discount()).isZero();
        assertThat(withoutPromotion.grandTotal()).isEqualByComparingTo(variant.getPrice());
        jdbc.update("update promotions set active = false where code = ?", "ORDER10");
        assertCode("PROMOTION_INACTIVE", () -> orders.createOrder(user, address("ORDER10")));
        assertThat(usage("ORDER10")).isZero();
        jdbc.update("update promotions set active = true where code = ?", "ORDER10");
        OrderResponse order = orders.createOrder(user, address("ORDER10"));
        assertThat(order.discount()).isEqualByComparingTo(expectedDiscount);
        assertThat(order.grandTotal()).isEqualByComparingTo(variant.getPrice().subtract(expectedDiscount));
        assertThat(jdbc.queryForObject("select promotion_code from orders where id = ?", String.class, order.id())).isEqualTo("ORDER10");
        assertThat(jdbc.queryForObject("select promotion_discount_amount from orders where id = ?", BigDecimal.class, order.id())).isEqualByComparingTo(expectedDiscount);
        jdbc.update("update promotions set name = ?, \"value\" = ? where code = ?", "Changed promotion", new BigDecimal("75.00"), "ORDER10");
        assertThat(jdbc.queryForObject("select promotion_discount_amount from orders where id = ?", BigDecimal.class, order.id())).isEqualByComparingTo(expectedDiscount);
        assertThat(usage("ORDER10")).isEqualTo(1);
        PaymentResponse payment = payments.createPayment(user, order.id(), new CreatePaymentRequest("SUCCEEDED"));
        assertThat(payment.amount()).isEqualByComparingTo(order.grandTotal());
    }

    @Test void failedOrderDoesNotConsumeAndConcurrentLastUsageIsNotOversubscribed() throws Exception {
        ProductVariant variant = variant();
        create("ONE", PromotionType.FIXED_AMOUNT, "10.00", null, null, 1, true, -60, 60);
        AuthenticatedUser failing = user("promotion-fail@example.com");
        when(cartStore.getQuantities("user:" + failing.id())).thenReturn(Map.of(variant.getId(), 999));
        assertThatThrownBy(() -> orders.createOrder(failing, address("ONE"))).isInstanceOf(RuntimeException.class);
        assertThat(usage("ONE")).isZero();
        AuthenticatedUser first = user("promotion-first@example.com"); AuthenticatedUser second = user("promotion-second@example.com");
        when(cartStore.getQuantities("user:" + first.id())).thenReturn(Map.of(variant.getId(), 1)); when(cartStore.getQuantities("user:" + second.id())).thenReturn(Map.of(variant.getId(), 1));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = executor.invokeAll(List.of(attempt(first), attempt(second)));
            long successes = results.stream().filter(result -> { try { return result.get(); } catch (Exception ignored) { return false; } }).count();
            assertThat(successes).isEqualTo(1); assertThat(usage("ONE")).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    private Callable<Boolean> attempt(AuthenticatedUser user) { return () -> { try { orders.createOrder(user, address("ONE")); return true; } catch (ConflictException ignored) { return false; } }; }
    private void assertCode(String expected, Runnable action) { assertThatThrownBy(action::run).isInstanceOf(ConflictException.class).extracting(error -> ((ConflictException) error).code()).isEqualTo(expected); }
    private PromotionResponse create(String code, PromotionType type, String value, String minimum, String cap, Integer limit, boolean active, long startsOffsetMinutes, long endsOffsetMinutes) { return promotions.create(request(code,type,value,minimum,cap,limit,active,startsOffsetMinutes,endsOffsetMinutes)); }
    private PromotionUpsertRequest request(String code, PromotionType type, String value, String minimum, String cap, Integer limit, boolean active, long startsOffsetMinutes, long endsOffsetMinutes) { Instant now=Instant.now(); return new PromotionUpsertRequest(code,"Promotion "+code,null,type,new BigDecimal(value),minimum==null?null:new BigDecimal(minimum),cap==null?null:new BigDecimal(cap),now.plusSeconds(startsOffsetMinutes*60),now.plusSeconds(endsOffsetMinutes*60),limit,active); }
    private CheckoutPreviewRequest address(String code) { return new CheckoutPreviewRequest("Lux Ora","+84901234567","1 Dong Khoi",null,"Ho Chi Minh City","Ho Chi Minh","Vietnam","700000",code); }
    private AuthenticatedUser user(String email) { User saved=users.save(new User(email,"hash","Lux","Ora")); return new AuthenticatedUser(saved.getId(),email,List.of("ROLE_USER")); }
    private ProductVariant variant() { return variants.findAll().stream().filter(v -> v.getSku().equals("AUR-X1-GRF-128")).findFirst().orElseThrow(); }
    private int usage(String code) { return jdbc.queryForObject("select usage_count from promotions where code = ?", Integer.class, code); }
}