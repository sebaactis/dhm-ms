package com.dmh.accountservice.dto;

import com.dmh.accountservice.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFilterRequest {

    private Transaction.TransactionType type;
    private AmountRange amountRange;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
