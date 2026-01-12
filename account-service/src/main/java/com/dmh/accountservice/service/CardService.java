package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.CardResponse;
import com.dmh.accountservice.dto.CreateCardRequest;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Card;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.exception.CardNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public CardService(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Obtiene todas las tarjetas asociadas a una cuenta.
     * 
     * @param accountId ID de la cuenta
     * @return Lista de tarjetas (puede estar vacía si no hay tarjetas)
     * @throws AccountNotFoundException si la cuenta no existe
     */
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByAccountId(Long accountId) {
        logger.info("Fetching cards for accountId: {}", accountId);

        // Verificar que la cuenta existe
        if (!accountRepository.existsById(accountId)) {
            logger.warn("Account not found with ID: {}", accountId);
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }

        List<Card> cards = cardRepository.findByAccountId(accountId);
        logger.info("Found {} cards for accountId: {}", cards.size(), accountId);

        return cards.stream()
                .map(CardResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una tarjeta específica que pertenezca a una cuenta.
     * 
     * @param accountId ID de la cuenta
     * @param cardId ID de la tarjeta
     * @return CardResponse con los datos de la tarjeta
     * @throws AccountNotFoundException si la cuenta no existe
     * @throws CardNotFoundException si la tarjeta no existe o no pertenece a la cuenta
     */
    @Transactional(readOnly = true)
    public CardResponse getCardByIdAndAccountId(Long accountId, Long cardId) {
        logger.info("Fetching card with ID: {} for accountId: {}", cardId, accountId);

        // Verificar que la cuenta existe
        if (!accountRepository.existsById(accountId)) {
            logger.warn("Account not found with ID: {}", accountId);
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }

        Card card = cardRepository.findByIdAndAccountId(cardId, accountId)
                .orElseThrow(() -> {
                    logger.warn("Card not found with ID: {} for accountId: {}", cardId, accountId);
                    return new CardNotFoundException(
                            "Card not found with ID: " + cardId + " for account: " + accountId);
                });

        logger.info("Card found: ID={}, Type={}, Brand={}", cardId, card.getCardType(), card.getCardBrand());
        return CardResponse.fromEntity(card);
    }

    /**
     * Crea una nueva tarjeta asociada a una cuenta.
     * (Para futuro CRUD completo)
     * 
     * @param accountId ID de la cuenta
     * @param request Datos de la tarjeta
     * @return CardResponse con la tarjeta creada
     */
    @Transactional
    public CardResponse createCard(Long accountId, CreateCardRequest request) {
        logger.info("Creating card for accountId: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        Card card = new Card();
        card.setAccount(account);
        card.setLastFourDigits(request.getLastFourDigits());
        card.setCardHolderName(request.getCardHolderName());
        card.setExpirationDate(request.getExpirationDate());
        card.setCardType(request.getCardType());
        card.setCardBrand(request.getCardBrand());
        card.setStatus(Card.CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(card);
        logger.info("Card created successfully: ID={}, Type={}, Brand={}", 
                savedCard.getId(), savedCard.getCardType(), savedCard.getCardBrand());

        return CardResponse.fromEntity(savedCard);
    }

    /**
     * Elimina una tarjeta (soft delete cambiando estado a BLOCKED).
     * 
     * @param accountId ID de la cuenta
     * @param cardId ID de la tarjeta
     * @return CardResponse con los datos de la tarjeta eliminada
     * @throws CardNotFoundException si la tarjeta no existe o no pertenece a la cuenta
     * @throws AccountNotFoundException si la cuenta no existe
     */
    @Transactional
    public CardResponse deleteCard(Long accountId, Long cardId) {
        logger.info("Deleting card with ID: {} for accountId: {}", cardId, accountId);

        // Verificar que la cuenta existe
        if (!accountRepository.existsById(accountId)) {
            logger.warn("Account not found with ID: {}", accountId);
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }

        Card card = cardRepository.findByIdAndAccountId(cardId, accountId)
                .orElseThrow(() -> {
                    logger.warn("Card not found with ID: {} for accountId: {}", cardId, accountId);
                    return new CardNotFoundException(
                            "Card not found with ID: " + cardId + " for account: " + accountId);
                });

        // Soft delete: cambiar estado a BLOCKED
        card.setStatus(Card.CardStatus.BLOCKED);
        Card deletedCard = cardRepository.save(card);

        logger.info("Card blocked successfully: ID={}, Type={}, Brand={}", 
                cardId, deletedCard.getCardType(), deletedCard.getCardBrand());

        return CardResponse.fromEntity(deletedCard);
    }
}
