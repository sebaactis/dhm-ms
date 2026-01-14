package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CardResponse;
import com.dmh.accountservice.dto.CreateCardRequest;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/cards")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);
    private final CardService cardService;
    private final AccountService accountService;

    public CardController(CardService cardService, AccountService accountService) {
        this.cardService = cardService;
        this.accountService = accountService;
    }

    private void validateAccountOwnership(Long accountId, Long authenticatedUserId) {
        AccountResponse account = accountService.getAccountById(accountId);
        
        if (!account.getUserId().equals(authenticatedUserId)) {
            logger.warn("User {} attempted to access cards of account {} (owned by user {})", 
                    authenticatedUserId, accountId, account.getUserId());
            throw new IllegalArgumentException("You can only access cards from your own account");
        }
        
        logger.debug("✅ Ownership validated: User {} owns account {}", authenticatedUserId, accountId);
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getAllCards(
            @PathVariable Long accountId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{}/cards - Fetching all cards", accountId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authenticatedUserId);
        
        List<CardResponse> cards = cardService.getCardsByAccountId(accountId);
        
        if (cards.isEmpty()) {
            logger.info("No cards found for accountId: {}", accountId);
        } else {
            logger.info("Found {} cards for accountId: {}", cards.size(), accountId);
        }
        
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable Long accountId,
            @PathVariable Long cardId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/accounts/{}/cards/{} - Fetching card", accountId, cardId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authenticatedUserId);
        
        CardResponse card = cardService.getCardByIdAndAccountId(accountId, cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long accountId,
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("POST /api/accounts/{}/cards - Creating new card", accountId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authenticatedUserId);
        
        CardResponse card = cardService.createCard(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<CardResponse> deleteCard(
            @PathVariable Long accountId,
            @PathVariable Long cardId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("DELETE /api/accounts/{}/cards/{} - Deleting card", accountId, cardId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authenticatedUserId);
        
        CardResponse deletedCard = cardService.deleteCard(accountId, cardId);
        logger.info("Card deleted successfully: ID={}, Status={}", cardId, deletedCard.getStatus());
        return ResponseEntity.ok(deletedCard);
    }
}
