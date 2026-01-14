package com.dmh.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de un depósito exitoso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositResponse {

    private Long transactionId;
    private Long accountId;
    private Long cardId;
    private BigDecimal amount;
    private String description;
    private String status;
    private BigDecimal newBalance;
    private LocalDateTime createdAt;
}
