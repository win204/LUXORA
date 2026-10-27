package com.luxora.commerce.loyalty.service;

import com.luxora.commerce.aws.ses.EmailService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.loyalty.dto.LoyaltyAccountResponse;
import com.luxora.commerce.loyalty.dto.LoyaltyTransactionResponse;
import com.luxora.commerce.loyalty.model.LoyaltyAccount;
import com.luxora.commerce.loyalty.model.LoyaltyTier;
import com.luxora.commerce.loyalty.model.LoyaltyTransaction;
import com.luxora.commerce.loyalty.model.LoyaltyTransactionType;
import com.luxora.commerce.loyalty.repository.LoyaltyAccountRepository;
import com.luxora.commerce.loyalty.repository.LoyaltyTransactionRepository;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Transactional
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public LoyaltyServiceImpl(LoyaltyAccountRepository loyaltyAccountRepository,
                              LoyaltyTransactionRepository loyaltyTransactionRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public LoyaltyAccountResponse getOrCreateAccount(UUID userId) {
        LoyaltyAccount account = getAccountEntity(userId);
        return toResponse(account);
    }

    @Override
    public LoyaltyAccount getAccountEntity(UUID userId) {
        return loyaltyAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
                    LoyaltyAccount newAccount = new LoyaltyAccount(user);
                    return loyaltyAccountRepository.save(newAccount);
                });
    }

    @Override
    public int calculatePointsToAward(BigDecimal grandTotal, LoyaltyTier tier) {
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        double multiplier = (tier != null) ? tier.getPointMultiplier() : 1.0;
        return (int) Math.floor(grandTotal.doubleValue() * multiplier);
    }

    @Override
    public void awardPointsOnDelivery(Order order) {
        if (order == null || order.getUser() == null) {
            return;
        }

        UUID userId = order.getUser().getId();
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getAccountEntity(userId));

        // Idempotency check: don't award points if already awarded for this order
        boolean alreadyAwarded = loyaltyTransactionRepository.existsByAccountIdAndOrderIdAndType(
                account.getId(), order.getId(), LoyaltyTransactionType.ORDER_EARNED);
        if (alreadyAwarded) {
            return;
        }

        LoyaltyTier oldTier = account.getTier();
        int points = calculatePointsToAward(order.getGrandTotal(), account.getTier());
        if (points > 0) {
            String shortId = order.getId().toString().substring(0, 8);
            account.addTransaction(
                    LoyaltyTransactionType.ORDER_EARNED,
                    points,
                    "Earned " + points + " points from delivered order #" + shortId,
                    order.getId()
            );
            loyaltyAccountRepository.save(account);

            LoyaltyTier newTier = account.getTier();
            if (newTier.ordinal() > oldTier.ordinal() && emailService != null) {
                String email = order.getCustomerEmail() != null ? order.getCustomerEmail() : (account.getUser() != null ? account.getUser().getEmail() : null);
                String customerName = order.getRecipientName() != null ? order.getRecipientName() : (account.getUser() != null ? account.getUser().getFullName() : "Valued Member");
                if (email != null && !email.isBlank()) {
                    emailService.sendTierUpgradeNotification(
                            email,
                            customerName,
                            oldTier.name(),
                            newTier.name(),
                            account.getCurrentPoints(),
                            newTier.getPointMultiplier(),
                            newTier.isFreeShipping()
                    );
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateLoyaltyDiscount(UUID userId, Integer pointsToRedeem, BigDecimal subtotal) {
        if (userId == null || pointsToRedeem == null || pointsToRedeem <= 0 || subtotal == null) {
            return BigDecimal.ZERO;
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId).orElse(null);
        if (account == null || account.getCurrentPoints() <= 0) {
            return BigDecimal.ZERO;
        }

        int usablePoints = Math.min(pointsToRedeem, account.getCurrentPoints());
        BigDecimal rawDiscount = BigDecimal.valueOf(usablePoints)
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);

        // Max discount allowed is 50% of order subtotal
        BigDecimal maxAllowedDiscount = subtotal.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        return rawDiscount.min(maxAllowedDiscount);
    }

    @Override
    public void redeemPointsForOrder(UUID userId, int points, Order order, BigDecimal discount) {
        if (userId == null || points <= 0 || order == null) {
            return;
        }

        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("LOYALTY_ACCOUNT_NOT_FOUND", "Loyalty account not found"));

        if (account.getCurrentPoints() < points) {
            throw new BadRequestException("INSUFFICIENT_LOYALTY_POINTS", "Not enough loyalty points available");
        }

        String shortId = order.getId().toString().substring(0, 8);
        account.addTransaction(
                LoyaltyTransactionType.CHECKOUT_REDEEMED,
                -points,
                "Redeemed " + points + " points for order #" + shortId,
                order.getId()
        );
        loyaltyAccountRepository.save(account);
    }

    @Override
    public void restorePointsForOrder(Order order) {
        if (order == null || order.getUser() == null || order.getLoyaltyPointsRedeemed() <= 0) {
            return;
        }

        UUID userId = order.getUser().getId();
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("LOYALTY_ACCOUNT_NOT_FOUND", "Loyalty account not found"));

        // Idempotency check: don't restore twice for same order
        boolean alreadyRestored = loyaltyTransactionRepository.existsByAccountIdAndOrderIdAndType(
                account.getId(), order.getId(), LoyaltyTransactionType.CANCEL_REFUNDED);
        if (alreadyRestored) {
            return;
        }

        int pointsToRestore = order.getLoyaltyPointsRedeemed();
        String shortId = order.getId().toString().substring(0, 8);
        account.addTransaction(
                LoyaltyTransactionType.CANCEL_REFUNDED,
                pointsToRestore,
                "Restored " + pointsToRestore + " points from cancelled order #" + shortId,
                order.getId()
        );
        loyaltyAccountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        LoyaltyAccount account = getAccountEntity(userId);
        return loyaltyTransactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(this::toTxResponse);
    }

    @Override
    public LoyaltyAccountResponse adjustPoints(UUID userId, int points, String reason) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> getAccountEntity(userId));

        LoyaltyTier oldTier = account.getTier();
        account.addTransaction(
                LoyaltyTransactionType.ADMIN_ADJUSTED,
                points,
                reason,
                null
        );

        LoyaltyAccount saved = loyaltyAccountRepository.save(account);
        LoyaltyTier newTier = saved.getTier();
        if (newTier.ordinal() > oldTier.ordinal() && emailService != null) {
            User user = saved.getUser();
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                String customerName = user.getFullName() != null ? user.getFullName() : "Valued Member";
                emailService.sendTierUpgradeNotification(
                        user.getEmail(),
                        customerName,
                        oldTier.name(),
                        newTier.name(),
                        saved.getCurrentPoints(),
                        newTier.getPointMultiplier(),
                        newTier.isFreeShipping()
                );
            }
        }
        return toResponse(saved);
    }

    private LoyaltyAccountResponse toResponse(LoyaltyAccount account) {
        BigDecimal dollarValue = BigDecimal.valueOf(account.getCurrentPoints())
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);

        LoyaltyTier tier = account.getTier();
        int nextTierPoints = tier.getNextTierPoints();
        int pointsToNextTier = Math.max(0, nextTierPoints - account.getLifetimePoints());

        return new LoyaltyAccountResponse(
                account.getId(),
                account.getUser().getId(),
                account.getCurrentPoints(),
                dollarValue,
                account.getLifetimePoints(),
                tier,
                tier.getPointMultiplier(),
                tier.isFreeShipping(),
                nextTierPoints,
                pointsToNextTier,
                account.getUpdatedAt()
        );
    }

    private LoyaltyTransactionResponse toTxResponse(LoyaltyTransaction tx) {
        return new LoyaltyTransactionResponse(
                tx.getId(),
                tx.getOrderId(),
                tx.getPoints(),
                tx.getType(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
