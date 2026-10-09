package com.grocerystore.api.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lets requests in only when they come through the site's front door. The CDN in front of the API adds a
 * secret header to every request it forwards, so calling the API's own address directly is refused.
 * With no secret configured (local runs, tests) everything is let through.
 */
@Component
public class OriginGuardFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Origin-Secret";

    private final byte[] secret;

    public OriginGuardFilter(StoreProperties properties) {
        String configured = properties.originSecret();
        this.secret = configured == null || configured.isBlank() ? null : configured.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        if (secret == null
                || (given != null && MessageDigest.isEqual(secret, given.getBytes(StandardCharsets.UTF_8)))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Use the store's own address.\",\"details\":null}");
    }
}
