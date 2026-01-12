package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.CardResponse;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Card;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.exception.CardNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardsByAccountId_ShouldReturnCards_WhenAccountExists() {
        // Arrange
        Long accountId = 1L;
        Account account = createTestAccount(accountId);
        
        Card card1 = createTestCard(1L, account, "1234", Card.CardType.DEBIT, Card.CardBrand.VISA);
        Card card2 = createTestCard(2L, account, "5678", Card.CardType.CREDIT, Card.CardBrand.MASTERCARD);
        
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByAccountId(accountId)).thenReturn(Arrays.asList(card1, card2));

        // Act
        List<CardResponse> cards = cardService.getCardsByAccountId(accountId);

        // Assert
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals("1234", cards.get(0).getLastFourDigits());
        assertEquals("5678", cards.get(1).getLastFourDigits());
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByAccountId(accountId);
    }

    @Test
    void getCardsByAccountId_ShouldReturnEmptyList_WhenNoCards() {
        // Arrange
        Long accountId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByAccountId(accountId)).thenReturn(Collections.emptyList());

        // Act
        List<CardResponse> cards = cardService.getCardsByAccountId(accountId);

        // Assert
        assertNotNull(cards);
        assertTrue(cards.isEmpty());
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByAccountId(accountId);
    }

    @Test
    void getCardsByAccountId_ShouldThrowException_WhenAccountNotFound() {
        // Arrange
        Long accountId = 999L;
        when(accountRepository.existsById(accountId)).thenReturn(false);

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            cardService.getCardsByAccountId(accountId);
        });
        verify(accountRepository).existsById(accountId);
        verify(cardRepository, never()).findByAccountId(any());
    }

    @Test
    void getCardByIdAndAccountId_ShouldReturnCard_WhenFound() {
        // Arrange
        Long accountId = 1L;
        Long cardId = 1L;
        Account account = createTestAccount(accountId);
        Card card = createTestCard(cardId, account, "1234", Card.CardType.DEBIT, Card.CardBrand.VISA);
        
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.of(card));

        // Act
        CardResponse result = cardService.getCardByIdAndAccountId(accountId, cardId);

        // Assert
        assertNotNull(result);
        assertEquals(cardId, result.getId());
        assertEquals("1234", result.getLastFourDigits());
        assertEquals(Card.CardType.DEBIT, result.getCardType());
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByIdAndAccountId(cardId, accountId);
    }

    @Test
    void getCardByIdAndAccountId_ShouldThrowException_WhenCardNotFound() {
        // Arrange
        Long accountId = 1L;
        Long cardId = 999L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> {
            cardService.getCardByIdAndAccountId(accountId, cardId);
        });
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByIdAndAccountId(cardId, accountId);
    }

    @Test
    void getCardByIdAndAccountId_ShouldThrowException_WhenAccountNotFound() {
        // Arrange
        Long accountId = 999L;
        Long cardId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(false);

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            cardService.getCardByIdAndAccountId(accountId, cardId);
        });
        verify(accountRepository).existsById(accountId);
        verify(cardRepository, never()).findByIdAndAccountId(any(), any());
    }

    @Test
    void deleteCard_ShouldBlockCard_WhenFound() {
        // Arrange
        Long accountId = 1L;
        Long cardId = 1L;
        Account account = createTestAccount(accountId);
        Card card = createTestCard(cardId, account, "1234", Card.CardType.DEBIT, Card.CardBrand.VISA);
        
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        // Act
        CardResponse result = cardService.deleteCard(accountId, cardId);

        // Assert
        assertNotNull(result);
        assertEquals(cardId, result.getId());
        assertEquals(Card.CardStatus.BLOCKED, card.getStatus());
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByIdAndAccountId(cardId, accountId);
        verify(cardRepository).save(card);
    }

    @Test
    void deleteCard_ShouldThrowException_WhenCardNotFound() {
        // Arrange
        Long accountId = 1L;
        Long cardId = 999L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(cardRepository.findByIdAndAccountId(cardId, accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> {
            cardService.deleteCard(accountId, cardId);
        });
        verify(accountRepository).existsById(accountId);
        verify(cardRepository).findByIdAndAccountId(cardId, accountId);
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deleteCard_ShouldThrowException_WhenAccountNotFound() {
        // Arrange
        Long accountId = 999L;
        Long cardId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(false);

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            cardService.deleteCard(accountId, cardId);
        });
        verify(accountRepository).existsById(accountId);
        verify(cardRepository, never()).findByIdAndAccountId(any(), any());
        verify(cardRepository, never()).save(any());
    }

    // Helper methods
    private Account createTestAccount(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setUserId(100L);
        account.setCvu("1234567890123456789012");
        account.setAlias("test.alias.account");
        return account;
    }

    private Card createTestCard(Long id, Account account, String lastFourDigits, 
                                Card.CardType cardType, Card.CardBrand cardBrand) {
        Card card = new Card();
        card.setId(id);
        card.setAccount(account);
        card.setLastFourDigits(lastFourDigits);
        card.setCardHolderName("John Doe");
        card.setExpirationDate(LocalDate.of(2025, 12, 31));
        card.setCardType(cardType);
        card.setCardBrand(cardBrand);
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }
}
