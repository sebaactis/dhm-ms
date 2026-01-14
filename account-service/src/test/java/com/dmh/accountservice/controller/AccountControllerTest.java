package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CreateAccountRequest;
import com.dmh.accountservice.exception.AccountAlreadyExistsException;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.fail-fast=false"
})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private com.dmh.accountservice.service.TransactionService transactionService;

    @MockBean
    private com.dmh.accountservice.util.JwtUtil jwtUtil;

    @Test
    void testCreateAccount_Success() throws Exception {
        String requestJson = """
            {
                "userId": 1
            }
            """;

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(
            AccountResponse.builder()
                .id(1L)
                .userId(1L)
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.cvu").value("1234567890123456789012"))
            .andExpect(jsonPath("$.alias").value("sol.luna.estrella"));
    }

    @Test
    void testCreateAccount_UnauthorizedUser_ThrowsException() throws Exception {
        String requestJson = """
            {
                "userId": 1
            }
            """;

        // Usuario autenticado (ID 2) intenta crear cuenta para usuario 1
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You can only create an account for yourself"));
    }

    @Test
    void testCreateAccount_AlreadyExists() throws Exception {
        String requestJson = """
            {
                "userId": 1
            }
            """;

        when(accountService.createAccount(any(CreateAccountRequest.class)))
            .thenThrow(new AccountAlreadyExistsException("Account already exists for user"));

        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isConflict());
    }

    @Test
    void testGetAccountByUserId_Success() throws Exception {
        Long userId = 1L;

        when(accountService.getAccountByUserId(userId)).thenReturn(
            AccountResponse.builder()
                .id(1L)
                .userId(userId)
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        mockMvc.perform(get("/api/accounts/user/{userId}", userId)
                .header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.cvu").value("1234567890123456789012"));
    }

    @Test
    void testGetAccountByUserId_UnauthorizedUser_ThrowsException() throws Exception {
        Long userId = 1L;

        // Usuario autenticado (ID 2) intenta acceder a cuenta de usuario 1
        mockMvc.perform(get("/api/accounts/user/{userId}", userId)
                .header("X-User-Id", "2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You can only access your own account"));
    }

    @Test
    void testGetAccountByUserId_NotFound() throws Exception {
        Long userId = 1L;
        when(accountService.getAccountByUserId(userId))
            .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/user/{userId}", userId)
                .header("X-User-Id", "1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetAccountById_Success() throws Exception {
        Long accountId = 1L;
        Long userId = 1L;

        when(accountService.getAccountById(accountId)).thenReturn(
            AccountResponse.builder()
                .id(accountId)
                .userId(userId)
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        mockMvc.perform(get("/api/accounts/{id}", accountId)
                .header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void testGetAccountById_UnauthorizedUser_ThrowsException() throws Exception {
        Long accountId = 1L;

        when(accountService.getAccountById(accountId)).thenReturn(
            AccountResponse.builder()
                .id(accountId)
                .userId(1L) // Cuenta pertenece a usuario 1
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        // Usuario autenticado (ID 2) intenta acceder a cuenta de usuario 1
        mockMvc.perform(get("/api/accounts/{id}", accountId)
                .header("X-User-Id", "2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You can only access your own account"));
    }

    @Test
    void testGetAccountTransactions_Success() throws Exception {
        Long accountId = 1L;
        Long userId = 1L;

        when(accountService.getAccountById(accountId)).thenReturn(
            AccountResponse.builder()
                .id(accountId)
                .userId(userId)
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        when(transactionService.getLastTransactions(accountId, 5)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                .header("X-User-Id", "1")
                .param("limit", "5"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetAccountTransactions_UnauthorizedUser_ThrowsException() throws Exception {
        Long accountId = 1L;

        when(accountService.getAccountById(accountId)).thenReturn(
            AccountResponse.builder()
                .id(accountId)
                .userId(1L) // Cuenta pertenece a usuario 1
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .balance(java.math.BigDecimal.ZERO)
                .build()
        );

        // Usuario autenticado (ID 2) intenta acceder a transacciones de cuenta de usuario 1
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                .header("X-User-Id", "2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You can only access your own account"));
    }
}
