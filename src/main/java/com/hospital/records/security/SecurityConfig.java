
package com.hospital.records.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            AppUserDetailsService userDetailsService) throws Exception {

        http
                // CSRF disabled for the stateless JWT API
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Allow H2 Console access during local development
                        .requestMatchers("/h2-console/**").permitAll()

                        // Public staff login
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Patient registration
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // All remaining endpoints require authentication
                        .anyRequest().authenticated())

                // Allow H2 Console frames
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))

                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // JWT authentication filter
                .addFilterBefore(
                        new JwtAuthFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
