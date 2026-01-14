package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.CreateTransferRequest;
import com.dmh.accountservice.dto.RecentTransferRecipient;
import com.dmh.accountservice.dto.TransferResponse;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.exception.ForbiddenAccessException;
import com.dmh.accountservice.exception.InsufficientFundsException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTransferTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = new Account();
        sourceAccount.setId(1L);
        sourceAccount.setUserId(100L);
        sourceAccount.setCvu("1234567890123456789012");
        sourceAccount.setAlias("usuario.alias.dos");
        sourceAccount.setBalance(new BigDecimal("1000.00"));

        destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setUserId(200L);
        destinationAccount.setCvu("0987654321098765432109");
        destinationAccount.setAlias("otro.alias.tres");
        destinationAccount.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void testPerformTransfer_Success() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("0987654321098765432109");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByCvuOrAlias(anyString(), anyString()))
                .thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    t.setId(1L);
                    t.setCreatedAt(LocalDateTime.now());
                    return t;
                });

        // Act
        TransferResponse response = transactionService.performTransfer(1L, request, 100L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getAccountId());
        assertEquals("0987654321098765432109", response.getDestination());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(new BigDecimal("900.00"), response.getNewBalance());
        assertEquals("COMPLETED", response.getStatus());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountService, times(1)).updateBalance(1L, new BigDecimal("900.00"));
        verify(accountService, times(1)).updateBalance(2L, new BigDecimal("600.00"));
    }

    @Test
    void testPerformTransfer_InsufficientFunds() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("0987654321098765432109");
        request.setAmount(new BigDecimal("2000.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () -> {
            transactionService.performTransfer(1L, request, 100L);
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testPerformTransfer_AccountNotFound() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("nonexistent");
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByCvuOrAlias(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            transactionService.performTransfer(1L, request, 100L);
        });
    }

    @Test
    void testPerformTransfer_ForbiddenAccess() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("0987654321098765432109");
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        assertThrows(ForbiddenAccessException.class, () -> {
            transactionService.performTransfer(1L, request, 999L); // Different user
        });
    }

    @Test
    void testPerformTransfer_SameAccount() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("1234567890123456789012"); // Same account's CVU
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByCvuOrAlias(anyString(), anyString()))
                .thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.performTransfer(1L, request, 100L);
        });
    }

    @Test
    void testGetRecentTransfers_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        Transaction transferOut = new Transaction();
        transferOut.setId(1L);
        transferOut.setDescription("Transfer to CVU: 0987654321098765432109");
        transferOut.setAmount(new BigDecimal("100.00"));
        transferOut.setCreatedAt(LocalDateTime.now());

        when(transactionRepository.findTransfersOutByAccountId(eq(1L), any()))
                .thenReturn(Arrays.asList(transferOut));

        // Act
        java.util.List<RecentTransferRecipient> recipients = transactionService.getRecentTransfers(1L, 5, 100L);

        // Assert
        assertNotNull(recipients);
        assertEquals(1, recipients.size());
        assertEquals("0987654321098765432109", recipients.get(0).getDestination());
        assertEquals(new BigDecimal("100.00"), recipients.get(0).getAmount());
        assertNotNull(recipients.get(0).getLastTransferDate(), "Last transfer date should not be null");
    }

    @Test
    void testGetRecentTransfers_ForbiddenAccess() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        assertThrows(ForbiddenAccessException.class, () -> {
            transactionService.getRecentTransfers(1L, 5, 999L); // Different user
        });
    }
}
