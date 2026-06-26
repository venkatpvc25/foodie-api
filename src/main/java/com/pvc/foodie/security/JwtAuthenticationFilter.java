package com.pvc.foodie.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            log.warn("Request continued without authentication because JWT is invalid: method={}, path={}",
                    request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);

        var userDetails = loadUserDetails(username, request);
        if (userDetails == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("JWT authentication set: username={}, method={}, path={}", username, request.getMethod(),
                request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    private org.springframework.security.core.userdetails.UserDetails loadUserDetails(
            String username,
            HttpServletRequest request) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (AuthenticationException ex) {
            log.warn("Request continued without authentication because user lookup failed: username={}, method={}, path={}, reason={}",
                    username, request.getMethod(), request.getRequestURI(), ex.getMessage());
            return null;
        }
    }
}
