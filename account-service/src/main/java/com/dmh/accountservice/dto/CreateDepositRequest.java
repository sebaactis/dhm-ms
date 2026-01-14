package com.dmh.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear un depósito desde una tarjeta a la cuenta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepositRequest {

    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}
