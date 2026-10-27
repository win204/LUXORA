package com.luxora.commerce.user.seed;

import com.luxora.commerce.promotion.model.Promotion;
import com.luxora.commerce.promotion.model.PromotionType;
import com.luxora.commerce.promotion.repository.PromotionRepository;
import com.luxora.commerce.user.model.Role;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.RoleRepository;
import com.luxora.commerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "luxora.seed.users", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserSeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeedDataLoader.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeedDataLoader(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PromotionRepository promotionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.promotionRepository = promotionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role adminRole = role("ROLE_ADMIN");
        Role userRole = role("ROLE_USER");

        seedAdminUser(adminRole, userRole);
        seedCustomerUser(userRole);
        seedSamplePromotions();
    }

    private Role role(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private void seedAdminUser(Role adminRole, Role userRole) {
        String adminEmail = "admin@luxora.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User(
                    adminEmail,
                    passwordEncoder.encode("Admin123456!"),
                    "Luxora",
                    "Administrator");
            admin.addRole(adminRole);
            admin.addRole(userRole);
            userRepository.save(admin);
            log.info("Seeded default admin user: {} (Password: Admin123456!)", adminEmail);
        }
    }

    private void seedCustomerUser(Role userRole) {
        String customerEmail = "customer@luxora.com";
        if (userRepository.findByEmail(customerEmail).isEmpty()) {
            User customer = new User(
                    customerEmail,
                    passwordEncoder.encode("Password123!"),
                    "Alex",
                    "Morgan");
            customer.addRole(userRole);
            userRepository.save(customer);
            log.info("Seeded default customer user: {} (Password: Password123!)", customerEmail);
        }
    }

    private void seedSamplePromotions() {
        if (!promotionRepository.existsByCode("LUXORA10")) {
            Promotion promo10 = new Promotion(
                    "LUXORA10",
                    "Giảm 10% cho đơn hàng đầu tiên",
                    "Áp dụng giảm 10% tối đa $100 cho đơn hàng từ $200",
                    PromotionType.PERCENTAGE,
                    new BigDecimal("10.00"),
                    new BigDecimal("200.00"),
                    new BigDecimal("100.00"),
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now().plus(365, ChronoUnit.DAYS),
                    500,
                    true);
            promotionRepository.save(promo10);
            log.info("Seeded sample promotion: LUXORA10");
        }

        if (!promotionRepository.existsByCode("WELCOME50")) {
            Promotion promo50 = new Promotion(
                    "WELCOME50",
                    "Giảm ngay $50 cho đơn hàng cao cấp",
                    "Áp dụng giảm trực tiếp $50 cho đơn hàng từ $300",
                    PromotionType.FIXED_AMOUNT,
                    new BigDecimal("50.00"),
                    new BigDecimal("300.00"),
                    new BigDecimal("50.00"),
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now().plus(365, ChronoUnit.DAYS),
                    200,
                    true);
            promotionRepository.save(promo50);
            log.info("Seeded sample promotion: WELCOME50");
        }
    }
}
