package com.threatadvisor.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 1 local security: in-memory users from environment.
 * Replace with JDBC/IdP in a later phase. Roles match the product spec.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api", "/api/health", "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.admin.username:admin}") String adminUser,
            @Value("${app.security.admin.password:admin_change_me}") String adminPassword,
            @Value("${app.security.analyst.username:analyst}") String analystUser,
            @Value("${app.security.analyst.password:analyst_change_me}") String analystPassword,
            @Value("${app.security.manager.username:manager}") String managerUser,
            @Value("${app.security.manager.password:manager_change_me}") String managerPassword,
            @Value("${app.security.viewer.username:viewer}") String viewerUser,
            @Value("${app.security.viewer.password:viewer_change_me}") String viewerPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUser).password(encoder.encode(adminPassword)).roles("ADMIN").build(),
                User.withUsername(analystUser).password(encoder.encode(analystPassword)).roles("SECURITY_ANALYST").build(),
                User.withUsername(managerUser).password(encoder.encode(managerPassword)).roles("SECURITY_MANAGER").build(),
                User.withUsername(viewerUser).password(encoder.encode(viewerPassword)).roles("VIEWER").build()
        );
    }
}
