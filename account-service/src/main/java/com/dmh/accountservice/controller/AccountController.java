package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CreateAccountRequest;
import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.dto.UpdateAccountRequest;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("POST /api/accounts - Creating account for userId: {}", request.getUserId());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> getAccountByUserId(@PathVariable Long userId) {
        logger.info("GET /api/accounts/user/{} - Fetching account", userId);
        AccountResponse response = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        logger.info("GET /api/accounts/{} - Fetching account by ID", id);
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        logger.info("GET /api/accounts/{}/transactions?limit={} - Fetching transactions", id, limit);
        List<TransactionResponse> transactions = transactionService.getLastTransactions(id, limit);
        return ResponseEntity.ok(transactions);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request) {
        logger.info("PATCH /api/accounts/{} - Updating account", id);
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(response);
    }
}
