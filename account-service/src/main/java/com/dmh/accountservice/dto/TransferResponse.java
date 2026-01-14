package com.dmh.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de una transferencia exitosa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private Long transactionId;
    private Long accountId;
    private String destination;
    private BigDecimal amount;
    private String description;
    private String status;
    private BigDecimal newBalance;
    private LocalDateTime createdAt;
}
