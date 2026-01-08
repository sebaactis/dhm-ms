package com.dmh.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar tokens invalidados (blacklist).
 * 
 * Cuando un usuario hace logout, su token se agrega a esta tabla.
 * El API Gateway verifica esta tabla antes de permitir acceso.
 * 
 * Los tokens se eliminan automáticamente cuando expiran (1 hora).
 */
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token JWT completo que fue invalidado.
     * Indexed para búsqueda rápida.
     */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /**
     * ID del usuario que hizo logout.
     * Útil para auditoría y debugging.
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Fecha y hora de expiración del token.
     * Después de esta fecha, el token puede eliminarse de la blacklist.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Fecha y hora en que el token fue agregado a la blacklist (logout).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
