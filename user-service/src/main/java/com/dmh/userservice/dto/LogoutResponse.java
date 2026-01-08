package com.dmh.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO para el logout exitoso.
 * 
 * Indica que el token fue invalidado correctamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {

    private String message;
    
    private Long userId;
}
