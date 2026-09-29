package com.luxora.commerce.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/brands").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/checkout/preview").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*/payments/latest").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/returns").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/returns").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/returns/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/returns/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/returns/*/mark-shipped").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/v1/cart", "/api/v1/cart/**").permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, RestAuthenticationEntryPoint authenticationEntryPoint) {
        return new JwtAuthenticationFilter(jwtService, authenticationEntryPoint);
    }
    @Bean
    UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException(username); };
    }
}



