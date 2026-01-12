package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Transaction testTransaction1;
    private Transaction testTransaction2;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUserId(100L);
        testAccount.setCvu("1234567890123456789012");
        testAccount.setAlias("test.alias.uno");
        testAccount.setBalance(BigDecimal.valueOf(1000.00));

        testTransaction1 = new Transaction();
        testTransaction1.setId(1L);
        testTransaction1.setAccount(testAccount);
        testTransaction1.setType(Transaction.TransactionType.DEPOSIT);
        testTransaction1.setAmount(BigDecimal.valueOf(500.00));
        testTransaction1.setDescription("Test deposit");
        testTransaction1.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction1.setCreatedAt(LocalDateTime.now().minusDays(1));

        testTransaction2 = new Transaction();
        testTransaction2.setId(2L);
        testTransaction2.setAccount(testAccount);
        testTransaction2.setType(Transaction.TransactionType.WITHDRAWAL);
        testTransaction2.setAmount(BigDecimal.valueOf(200.00));
        testTransaction2.setDescription("Test withdrawal");
        testTransaction2.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction2.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should return last N transactions when account exists")
    void shouldReturnLastTransactionsWhenAccountExists() {
        // Given
        Long accountId = 1L;
        Integer limit = 5;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(Arrays.asList(testTransaction2, testTransaction1));

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, limit);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountId()).isEqualTo(accountId);
        assertThat(result.get(0).getType()).isEqualTo("WITHDRAWAL");
        assertThat(result.get(1).getType()).isEqualTo("DEPOSIT");
    }

    @Test
    @DisplayName("Should use default limit when null is provided")
    void shouldUseDefaultLimitWhenNullProvided() {
        // Given
        Long accountId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), eq(PageRequest.of(0, 5))))
                .thenReturn(List.of(testTransaction1));

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, null);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        Long accountId = 999L;
        when(accountRepository.existsById(accountId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> transactionService.getLastTransactions(accountId, 5))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactions() {
        // Given
        Long accountId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(List.of());

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, 5);

        // Then
        assertThat(result).isEmpty();
    }
}
