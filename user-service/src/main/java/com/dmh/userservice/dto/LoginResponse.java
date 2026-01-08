package com.dmh.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO para el login exitoso.
 * 
 * Contiene:
 * - token: JWT token para autenticación
 * - userId: ID del usuario logueado
 * - email: Email del usuario
 * - expiresIn: Tiempo de expiración en milisegundos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    
    private Long userId;
    
    private String email;
    
    private Long expiresIn;  // Milisegundos hasta expiración (3600000 = 1 hora)
}
