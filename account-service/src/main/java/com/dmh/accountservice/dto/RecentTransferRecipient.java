package com.dmh.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar un destinatario reciente de transferencias.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentTransferRecipient {

    private String destination;
    private BigDecimal amount;
    private LocalDateTime lastTransferDate;
}
