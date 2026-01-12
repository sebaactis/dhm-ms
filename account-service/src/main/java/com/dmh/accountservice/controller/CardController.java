package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CardResponse;
import com.dmh.accountservice.dto.CreateCardRequest;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.CardService;
import com.dmh.accountservice.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para operaciones CRUD de tarjetas.
 * 
 * Endpoints:
 * - GET /api/accounts/{accountId}/cards - Listar todas las tarjetas de una cuenta
 * - GET /api/accounts/{accountId}/cards/{cardId} - Obtener tarjeta específica
 * - POST /api/accounts/{accountId}/cards - Crear nueva tarjeta
 * - DELETE /api/accounts/{accountId}/cards/{cardId} - Eliminar tarjeta
 * 
 * Seguridad:
 * - Todos los endpoints validan que el usuario del JWT sea owner de la cuenta
 */
@RestController
@RequestMapping("/api/accounts/{accountId}/cards")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);
    private final CardService cardService;
    private final AccountService accountService;
    private final JwtUtil jwtUtil;

    public CardController(CardService cardService, AccountService accountService, JwtUtil jwtUtil) {
        this.cardService = cardService;
        this.accountService = accountService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Valida que la cuenta pertenezca al usuario del JWT.
     * @throws IllegalArgumentException si la cuenta no pertenece al usuario
     */
    private void validateAccountOwnership(Long accountId, String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        Long requestingUserId = jwtUtil.extractUserId(token);

        AccountResponse account = accountService.getAccountById(accountId);
        
        if (!account.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to access cards of account {} (owned by user {})", 
                    requestingUserId, accountId, account.getUserId());
            throw new IllegalArgumentException("You can only access cards from your own account");
        }
        
        logger.debug("✅ Ownership validated: User {} owns account {}", requestingUserId, accountId);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }
        return authHeader.substring(7);
    }

    /**
     * Obtiene todas las tarjetas asociadas a una cuenta.
     * 
     * Respuestas:
     * - 200 OK: Lista de tarjetas (puede estar vacía [])
     * - 403 FORBIDDEN: Usuario no es owner de la cuenta
     * - 404 NOT FOUND: Cuenta no existe
     * 
     * @param accountId ID de la cuenta
     * @param authHeader JWT token del usuario
     * @return Lista de tarjetas (vacía si no hay tarjetas)
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> getAllCards(
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("GET /api/accounts/{}/cards - Fetching all cards", accountId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authHeader);
        
        List<CardResponse> cards = cardService.getCardsByAccountId(accountId);
        
        if (cards.isEmpty()) {
            logger.info("No cards found for accountId: {}", accountId);
        } else {
            logger.info("Found {} cards for accountId: {}", cards.size(), accountId);
        }
        
        return ResponseEntity.ok(cards);
    }

    /**
     * Obtiene una tarjeta específica de una cuenta.
     * 
     * Respuestas:
     * - 200 OK: Datos de la tarjeta
     * - 403 FORBIDDEN: Usuario no es owner de la cuenta
     * - 404 NOT FOUND: Tarjeta no existe o no pertenece a la cuenta
     * 
     * @param accountId ID de la cuenta
     * @param cardId ID de la tarjeta
     * @param authHeader JWT token del usuario
     * @return Datos de la tarjeta
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable Long accountId,
            @PathVariable Long cardId,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("GET /api/accounts/{}/cards/{} - Fetching card", accountId, cardId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authHeader);
        
        CardResponse card = cardService.getCardByIdAndAccountId(accountId, cardId);
        return ResponseEntity.ok(card);
    }

    /**
     * Crea una nueva tarjeta para una cuenta.
     * 
     * Respuestas:
     * - 201 CREATED: Tarjeta creada exitosamente
     * - 403 FORBIDDEN: Usuario no es owner de la cuenta
     * - 404 NOT FOUND: Cuenta no existe
     * 
     * @param accountId ID de la cuenta
     * @param request Datos de la tarjeta
     * @param authHeader JWT token del usuario
     * @return Tarjeta creada
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long accountId,
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/accounts/{}/cards - Creating new card", accountId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authHeader);
        
        CardResponse card = cardService.createCard(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Elimina (bloquea) una tarjeta.
     * 
     * Respuestas:
     * - 200 OK: Tarjeta eliminada correctamente
     * - 403 FORBIDDEN: Usuario no es owner de la cuenta
     * - 404 NOT FOUND: Tarjeta no existe o no pertenece a la cuenta
     * 
     * @param accountId ID de la cuenta
     * @param cardId ID de la tarjeta
     * @param authHeader JWT token del usuario
     * @return 200 OK con datos de la tarjeta eliminada
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<CardResponse> deleteCard(
            @PathVariable Long accountId,
            @PathVariable Long cardId,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("DELETE /api/accounts/{}/cards/{} - Deleting card", accountId, cardId);
        
        // Validar ownership de la cuenta
        validateAccountOwnership(accountId, authHeader);
        
        CardResponse deletedCard = cardService.deleteCard(accountId, cardId);
        logger.info("Card deleted successfully: ID={}, Status={}", cardId, deletedCard.getStatus());
        return ResponseEntity.ok(deletedCard);
    }
}
