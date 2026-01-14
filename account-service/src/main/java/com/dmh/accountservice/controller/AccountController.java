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

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("POST /api/accounts - Creating account for userId: {}", request.getUserId());

        // Validar que el usuario autenticado solo puede crear cuenta para sí mismo
        if (!request.getUserId().equals(authenticatedUserId)) {
            logger.warn("User {} attempted to create account for user {}", 
                       authenticatedUserId, request.getUserId());
            throw new IllegalArgumentException("You can only create an account for yourself");
        }

        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> getAccountByUserId(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/user/{} - Fetching account", userId);

        // Validar que el usuario autenticado solo puede ver su propia cuenta
        if (!userId.equals(authenticatedUserId)) {
            logger.warn("User {} attempted to access account of user {}", 
                       authenticatedUserId, userId);
            throw new IllegalArgumentException("You can only access your own account");
        }

        AccountResponse response = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{} - Fetching account by ID", id);

        // Validar ownership usando el método helper
        validateAccountOwnership(id, authenticatedUserId);

        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{}/transactions?limit={} - Fetching transactions", id, limit);

        // Validar ownership de la cuenta antes de mostrar transacciones
        validateAccountOwnership(id, authenticatedUserId);

        List<TransactionResponse> transactions = transactionService.getLastTransactions(id, limit);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/transferences")
    public ResponseEntity<List<RecentTransferRecipient>> getRecentTransfers(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{}/transferences?limit={} - Fetching recent transfers", id, limit);

        List<RecentTransferRecipient> recipients = transactionService.getRecentTransfers(id, limit, authenticatedUserId);
        return ResponseEntity.ok(recipients);
    }

    @PostMapping("/{id}/transfers")
    public ResponseEntity<TransferResponse> performTransfer(
            @PathVariable Long id,
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("POST /api/accounts/{}/transfers - Performing transfer to {}, amount: {}", 
                   id, request.getDestination(), request.getAmount());

        TransferResponse response = transactionService.performTransfer(id, request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<TransactionResponse>> getAccountActivity(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) AmountRange amountRange,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        logger.info("GET /api/accounts/{}/activity - Fetching activity with filters: type={}, amountRange={}, dateFrom={}, dateTo={}", 
                    id, type, amountRange, dateFrom, dateTo);

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

        List<TransactionResponse> activity = transactionService.getAllActivity(id, authenticatedUserId, filters);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/{id}/activity/{transactionId}")
    public ResponseEntity<TransactionResponse> getActivityDetail(
            @PathVariable Long id,
            @PathVariable Long transactionId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{}/activity/{} - Fetching activity detail", id, transactionId);

        TransactionResponse detail = transactionService.getActivityDetail(id, transactionId, authenticatedUserId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<DepositResponse> createDeposit(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepositRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("POST /api/accounts/{}/deposit - Creating deposit from card {}", id, request.getCardId());

        DepositResponse response = transactionService.createDeposit(id, request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("PATCH /api/accounts/{} - Updating account", id);

        // Validar ownership antes de permitir actualización
        validateAccountOwnership(id, authenticatedUserId);

        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Valida que el usuario autenticado es el dueño de la cuenta especificada
     * @param accountId ID de la cuenta a validar
     * @param authenticatedUserId ID del usuario autenticado (desde X-User-Id header)
     * @throws IllegalArgumentException si el usuario no es el dueño de la cuenta
     */
    private void validateAccountOwnership(Long accountId, Long authenticatedUserId) {
        AccountResponse account = accountService.getAccountById(accountId);
        
        if (!account.getUserId().equals(authenticatedUserId)) {
            logger.warn("User {} attempted to access account {} (owned by user {})", 
                       authenticatedUserId, accountId, account.getUserId());
            throw new IllegalArgumentException("You can only access your own account");
        }
        
        logger.debug("✅ Ownership validated: User {} owns account {}", authenticatedUserId, accountId);
    }
}
