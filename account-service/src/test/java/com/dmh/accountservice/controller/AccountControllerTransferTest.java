package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.CreateTransferRequest;
import com.dmh.accountservice.dto.RecentTransferRecipient;
import com.dmh.accountservice.dto.TransferResponse;
import com.dmh.accountservice.exception.ForbiddenAccessException;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.TransactionService;
import com.dmh.accountservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTransferTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AccountController accountController;

    private final String validToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

    @BeforeEach
    void setUp() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(100L);
    }

    @Test
    void testGetRecentTransfers_Success() {
        // Arrange
        RecentTransferRecipient recipient1 = RecentTransferRecipient.builder()
                .destination("0987654321098765432109")
                .amount(BigDecimal.valueOf(100.00))
                .lastTransferDate(LocalDateTime.now())
                .build();

        when(transactionService.getRecentTransfers(anyLong(), anyInt(), anyLong()))
                .thenReturn(Arrays.asList(recipient1));

        // Act
        ResponseEntity<List<RecentTransferRecipient>> response =
                accountController.getRecentTransfers(1L, 5, validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("0987654321098765432109", response.getBody().get(0).getDestination());
    }

    @Test
    void testGetRecentTransfers_ForbiddenAccess() {
        // Arrange
        when(transactionService.getRecentTransfers(anyLong(), anyInt(), anyLong()))
                .thenThrow(new ForbiddenAccessException("You do not have permission"));

        // Act & Assert
        assertThrows(ForbiddenAccessException.class, () -> {
            accountController.getRecentTransfers(1L, 5, validToken);
        });
    }

    @Test
    void testPerformTransfer_Success() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("0987654321098765432109");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setDescription("Test transfer");

        TransferResponse transferResponse = TransferResponse.builder()
                .transactionId(1L)
                .accountId(1L)
                .destination("0987654321098765432109")
                .amount(BigDecimal.valueOf(100.00))
                .description("Test transfer")
                .status("COMPLETED")
                .newBalance(BigDecimal.valueOf(900.00))
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.performTransfer(anyLong(), any(CreateTransferRequest.class), anyLong()))
                .thenReturn(transferResponse);

        // Act
        ResponseEntity<TransferResponse> response =
                accountController.performTransfer(1L, request, validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getTransactionId());
        assertEquals("0987654321098765432109", response.getBody().getDestination());
        assertEquals(BigDecimal.valueOf(100.00), response.getBody().getAmount());
        assertEquals("COMPLETED", response.getBody().getStatus());
    }

    @Test
    void testPerformTransfer_InsufficientFunds() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("0987654321098765432109");
        request.setAmount(BigDecimal.valueOf(2000.00));

        when(transactionService.performTransfer(anyLong(), any(CreateTransferRequest.class), anyLong()))
                .thenThrow(new com.dmh.accountservice.exception.InsufficientFundsException(
                        "Insufficient funds"));

        // Act & Assert
        assertThrows(com.dmh.accountservice.exception.InsufficientFundsException.class, () -> {
            accountController.performTransfer(1L, request, validToken);
        });
    }

    @Test
    void testPerformTransfer_AccountNotFound() {
        // Arrange
        CreateTransferRequest request = new CreateTransferRequest();
        request.setDestination("nonexistent");
        request.setAmount(BigDecimal.valueOf(100.00));

        when(transactionService.performTransfer(anyLong(), any(CreateTransferRequest.class), anyLong()))
                .thenThrow(new com.dmh.accountservice.exception.AccountNotFoundException(
                        "Account not found"));

        // Act & Assert
        assertThrows(com.dmh.accountservice.exception.AccountNotFoundException.class, () -> {
            accountController.performTransfer(1L, request, validToken);
        });
    }
}
