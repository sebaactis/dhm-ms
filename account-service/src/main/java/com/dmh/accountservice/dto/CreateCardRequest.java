package com.dmh.accountservice.dto;

import com.dmh.accountservice.entity.Card;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para crear una nueva tarjeta.
 * IMPORTANTE: En producción, los datos sensibles (número completo, CVV) 
 * deberían manejarse con vault/HSM y tokenización.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

    /**
     * Solo últimos 4 dígitos para almacenar.
     * En producción real, el número completo viene tokenizado.
     */
    @NotBlank(message = "Last four digits cannot be empty")
    @Pattern(regexp = "^\\d{4}$", message = "Last four digits must be exactly 4 digits")
    private String lastFourDigits;

    @NotBlank(message = "Card holder name cannot be empty")
    @Size(max = 100, message = "Card holder name too long")
    private String cardHolderName;

    @NotNull(message = "Expiration date cannot be null")
    private LocalDate expirationDate;

    @NotNull(message = "Card type cannot be null")
    private Card.CardType cardType;

    @NotNull(message = "Card brand cannot be null")
    private Card.CardBrand cardBrand;
}
