package com.redlining.config;

import com.redlining.security.AdminAccessFilter;
import com.redlining.security.AuthEntryPoint;
import com.redlining.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AdminAccessFilter adminAccessFilter;
    private final AuthEntryPoint authEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AdminAccessFilter adminAccessFilter, AuthEntryPoint authEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.adminAccessFilter = adminAccessFilter;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/captcha", "/api/register", "/api/login").permitAll()
                        .requestMatchers("/api/documents/callback", "/api/documents/file", "/api/preview/file").permitAll()
                        .requestMatchers("/api/payment/wechat/notify").permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminAccessFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
