package com.dmh.accountservice.dto;

import com.dmh.accountservice.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private Long accountId;
    private String type;
    private BigDecimal amount;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
