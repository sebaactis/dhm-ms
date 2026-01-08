package com.dmh.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidad para generar y validar JWT tokens.
 * 
 * Responsabilidades:
 * - Generar tokens JWT con información del usuario
 * - Validar tokens (firma, expiración)
 * - Extraer información del token (claims)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Genera la clave de firma a partir del secret en Base64.
     * Usa HMAC-SHA256 para firmar el token.
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token JWT para un usuario.
     * 
     * @param email Email del usuario (subject)
     * @param userId ID del usuario
     * @return Token JWT firmado
     */
    public String generateToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        
        return createToken(claims, email);
    }

    /**
     * Crea el token JWT con los claims y subject.
     * 
     * Estructura del token:
     * - Header: algoritmo (HS256) y tipo (JWT)
     * - Payload: claims (userId, email), subject, issued at, expiration
     * - Signature: firma con secret key
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)                    // Datos custom (userId, email)
                .setSubject(subject)                  // Subject = email
                .setIssuedAt(now)                     // Fecha de emisión
                .setExpiration(expirationDate)        // Fecha de expiración (1 hora)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // Firma con HMAC-SHA256
                .compact();
    }

    /**
     * Extrae el email (subject) del token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el userId del token.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token usando una función.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     * Valida la firma usando la secret key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Verifica si el token ha expirado.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valida que el token sea válido:
     * - El email coincide
     * - No ha expirado
     * - La firma es válida (se valida en extractAllClaims)
     */
    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    /**
     * Valida solo que el token sea válido (firma y expiración).
     * No verifica el email.
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);  // Si no lanza excepción, es válido
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
