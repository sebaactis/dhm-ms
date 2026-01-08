package com.dmh.userservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "testSecretKey12345678901234567890";
    private static final Long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken("test@example.com", 1L);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateToken("test@example.com", 1L);
        assertTrue(jwtUtil.validateToken(token, "test@example.com"));
    }

    @Test
    void testExtractUserId() {
        Long userId = 1L;
        String token = jwtUtil.generateToken("test@example.com", userId);
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void testExtractEmail() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, 1L);
        assertEquals(email, jwtUtil.extractEmail(token));
    }

    @Test
    void testValidateTokenWithoutEmail() {
        String token = jwtUtil.generateToken("test@example.com", 1L);
        assertTrue(jwtUtil.validateToken(token));
    }
}
