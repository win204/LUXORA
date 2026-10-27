package com.luxora.commerce.user.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luxora.commerce.promotion.model.Promotion;
import com.luxora.commerce.promotion.repository.PromotionRepository;
import com.luxora.commerce.user.model.Role;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.RoleRepository;
import com.luxora.commerce.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserSeedDataLoaderUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PromotionRepository promotionRepository;

    private PasswordEncoder passwordEncoder;
    private UserSeedDataLoader loader;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        loader = new UserSeedDataLoader(userRepository, roleRepository, promotionRepository, passwordEncoder);
    }

    @Test
    @DisplayName("UserSeedDataLoader creates default admin, customer and promotions when absent")
    void seedsUsersAndPromotionsWhenAbsent() {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.findByEmail("admin@luxora.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("customer@luxora.com")).thenReturn(Optional.empty());
        when(promotionRepository.existsByCode("LUXORA10")).thenReturn(false);
        when(promotionRepository.existsByCode("WELCOME50")).thenReturn(false);

        loader.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(userCaptor.capture());

        User admin = userCaptor.getAllValues().get(0);
        assertThat(admin.getEmail()).isEqualTo("admin@luxora.com");
        assertThat(admin.getFirstName()).isEqualTo("Luxora");
        assertThat(admin.getLastName()).isEqualTo("Administrator");
        assertThat(passwordEncoder.matches("Admin123456!", admin.getPasswordHash())).isTrue();

        User customer = userCaptor.getAllValues().get(1);
        assertThat(customer.getEmail()).isEqualTo("customer@luxora.com");
        assertThat(customer.getFirstName()).isEqualTo("Alex");
        assertThat(customer.getLastName()).isEqualTo("Morgan");
        assertThat(passwordEncoder.matches("Password123!", customer.getPasswordHash())).isTrue();

        ArgumentCaptor<Promotion> promoCaptor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository, org.mockito.Mockito.times(2)).save(promoCaptor.capture());

        Promotion promo10 = promoCaptor.getAllValues().get(0);
        assertThat(promo10.getCode()).isEqualTo("LUXORA10");
        assertThat(promo10.isActive()).isTrue();

        Promotion promo50 = promoCaptor.getAllValues().get(1);
        assertThat(promo50.getCode()).isEqualTo("WELCOME50");
        assertThat(promo50.isActive()).isTrue();
    }

    @Test
    @DisplayName("UserSeedDataLoader skips creation when users and promotions already exist")
    void skipsWhenAlreadyPresent() {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.findByEmail("admin@luxora.com")).thenReturn(Optional.of(new User("admin@luxora.com", "hash", "Admin", "User")));
        when(userRepository.findByEmail("customer@luxora.com")).thenReturn(Optional.of(new User("customer@luxora.com", "hash", "Customer", "User")));
        when(promotionRepository.existsByCode("LUXORA10")).thenReturn(true);
        when(promotionRepository.existsByCode("WELCOME50")).thenReturn(true);

        loader.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any());
        verify(promotionRepository, never()).save(any());
    }
}
