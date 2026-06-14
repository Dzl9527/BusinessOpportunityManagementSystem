package com.boms.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserId_AuthenticatedUser() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user123", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String userId = SecurityUtils.getCurrentUserId();
        assertEquals("user123", userId);
    }

    @Test
    void testGetCurrentUserId_NoAuthentication() {
        String userId = SecurityUtils.getCurrentUserId();
        assertNull(userId);
    }
}
