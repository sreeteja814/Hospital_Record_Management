
package com.hospital.records.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads "Authorization: Bearer <token>", validates it and, if valid,
 * marks the request as authenticated.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService,
                         AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // Skip JWT authentication for H2 Console requests.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER_PREFIX.length()).trim();

            try {
                String email = jwtService.extractSubject(token);
                UserDetails user = userDetailsService.loadUserByUsername(email);

                if (user.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user, null, user.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }

            } catch (JwtException | IllegalArgumentException
                     | UsernameNotFoundException ex) {
                logger.debug("Rejected JWT: " + ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
