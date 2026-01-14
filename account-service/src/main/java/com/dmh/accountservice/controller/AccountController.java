package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.ActivityFilterRequest;
import com.dmh.accountservice.dto.AmountRange;
import com.dmh.accountservice.dto.CreateAccountRequest;
import com.dmh.accountservice.dto.CreateDepositRequest;
import com.dmh.accountservice.dto.CreateTransferRequest;
import com.dmh.accountservice.dto.DepositResponse;
import com.dmh.accountservice.dto.RecentTransferRecipient;
import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.dto.TransferResponse;
import com.dmh.accountservice.dto.UpdateAccountRequest;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.TransactionService;
import com.dmh.accountservice.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final JwtUtil jwtUtil;

    public AccountController(AccountService accountService, TransactionService transactionService, JwtUtil jwtUtil) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.jwtUtil = jwtUtil;
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

    @GetMapping("/{id}/transferences")
    public ResponseEntity<List<RecentTransferRecipient>> getRecentTransfers(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("GET /api/accounts/{}/transferences?limit={} - Fetching recent transfers", id, limit);

        // Extraer userId del token JWT
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        List<RecentTransferRecipient> recipients = transactionService.getRecentTransfers(id, limit, requestingUserId);
        return ResponseEntity.ok(recipients);
    }

    @PostMapping("/{id}/transfers")
    public ResponseEntity<TransferResponse> performTransfer(
            @PathVariable Long id,
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/accounts/{}/transfers - Performing transfer to {}, amount: {}", 
                   id, request.getDestination(), request.getAmount());

        // Extraer userId del token JWT
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        TransferResponse response = transactionService.performTransfer(id, request, requestingUserId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<TransactionResponse>> getAccountActivity(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) AmountRange amountRange,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        logger.info("GET /api/accounts/{}/activity - Fetching activity with filters: type={}, amountRange={}, dateFrom={}, dateTo={}", 
                    id, type, amountRange, dateFrom, dateTo);

        // Extraer userId del token JWT
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        // Construir filtros si se proporcionaron
        ActivityFilterRequest filters = null;
        if (type != null || amountRange != null || dateFrom != null || dateTo != null) {
            filters = ActivityFilterRequest.builder()
                    .type(type)
                    .amountRange(amountRange)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .build();
        }

        List<TransactionResponse> activity = transactionService.getAllActivity(id, requestingUserId, filters);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/{id}/activity/{transactionId}")
    public ResponseEntity<TransactionResponse> getActivityDetail(
            @PathVariable Long id,
            @PathVariable Long transactionId,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("GET /api/accounts/{}/activity/{} - Fetching activity detail", id, transactionId);

        // Extraer userId del token JWT
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        TransactionResponse detail = transactionService.getActivityDetail(id, transactionId, requestingUserId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<DepositResponse> createDeposit(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepositRequest request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/accounts/{}/transferences - Creating deposit from card {}", id, request.getCardId());

        // Extraer userId del token JWT
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        DepositResponse response = transactionService.createDeposit(id, request, requestingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request) {
        logger.info("PATCH /api/accounts/{} - Updating account", id);
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }
        return authHeader.substring(7);
    }
}
