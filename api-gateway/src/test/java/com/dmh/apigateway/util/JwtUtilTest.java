package com.dmh.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "testSecretKey12345678901234567890";
    private static final long EXPIRATION = 3600000L;
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String generateTestToken(String email, Long userId, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void testExtractEmail() {
        String email = "test@example.com";
        String token = generateTestToken(email, 1L, EXPIRATION);

        String extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testExtractUserId() {
        Long userId = 123L;
        String token = generateTestToken("test@example.com", userId, EXPIRATION);

        Long extractedUserId = jwtUtil.extractUserId(token);
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractExpiration() {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);
        String token = generateTestToken("test@example.com", 1L, EXPIRATION);

        Date extractedExpiration = jwtUtil.extractExpiration(token);
        assertNotNull(extractedExpiration);
        assertEquals(expiryDate.getTime() / 1000, extractedExpiration.getTime() / 1000);
    }

    @Test
    void testValidateToken_Valid() {
        String token = generateTestToken("test@example.com", 1L, EXPIRATION);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        String token = "invalid.token.here";

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Expired() {
        String token = generateTestToken("test@example.com", 1L, -1000);

        assertFalse(jwtUtil.validateToken(token));
    }
}
