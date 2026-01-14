package com.dmh.accountservice.controller;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CardResponse;
import com.dmh.accountservice.dto.CreateCardRequest;
import com.dmh.accountservice.service.AccountService;
import com.dmh.accountservice.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CardController cardController;

    @Test
    void testGetAllCards_Success() {
        // Arrange
        Long accountId = 1L;
        Long authenticatedUserId = 100L;

        AccountResponse accountResponse = AccountResponse.builder()
                .id(accountId)
                .userId(authenticatedUserId)
                .build();

        CardResponse card1 = CardResponse.builder()
                .id(1L)
                .accountId(accountId)
                .lastFourDigits("3456")
                .maskedNumber("**** **** **** 3456")
                .build();

        CardResponse card2 = CardResponse.builder()
                .id(2L)
                .accountId(accountId)
                .lastFourDigits("7654")
                .maskedNumber("**** **** **** 7654")
                .build();

        when(accountService.getAccountById(accountId)).thenReturn(accountResponse);
        when(cardService.getCardsByAccountId(accountId)).thenReturn(Arrays.asList(card1, card2));

        // Act
        ResponseEntity<List<CardResponse>> response = cardController.getAllCards(accountId, authenticatedUserId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(accountService, times(1)).getAccountById(accountId);
        verify(cardService, times(1)).getCardsByAccountId(accountId);
    }

    @Test
    void testGetAllCards_Unauthorized() {
        // Arrange
        Long accountId = 1L;
        Long authenticatedUserId = 100L;
        Long differentUserId = 200L;

        AccountResponse accountResponse = AccountResponse.builder()
                .id(accountId)
                .userId(differentUserId) // Account owned by different user
                .build();

        when(accountService.getAccountById(accountId)).thenReturn(accountResponse);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cardController.getAllCards(accountId, authenticatedUserId);
        });

        assertEquals("You can only access cards from your own account", exception.getMessage());
        verify(accountService, times(1)).getAccountById(accountId);
        verify(cardService, never()).getCardsByAccountId(anyLong());
    }

    @Test
    void testCreateCard_Success() {
        // Arrange
        Long accountId = 1L;
        Long authenticatedUserId = 100L;

        AccountResponse accountResponse = AccountResponse.builder()
                .id(accountId)
                .userId(authenticatedUserId)
                .build();

        CreateCardRequest request = new CreateCardRequest();
        request.setLastFourDigits("3456");
        request.setCardHolderName("John Doe");

        CardResponse cardResponse = CardResponse.builder()
                .id(1L)
                .accountId(accountId)
                .lastFourDigits("3456")
                .maskedNumber("**** **** **** 3456")
                .build();

        when(accountService.getAccountById(accountId)).thenReturn(accountResponse);
        when(cardService.createCard(accountId, request)).thenReturn(cardResponse);

        // Act
        ResponseEntity<CardResponse> response = cardController.createCard(accountId, request, authenticatedUserId);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(accountId, response.getBody().getAccountId());
        verify(accountService, times(1)).getAccountById(accountId);
        verify(cardService, times(1)).createCard(accountId, request);
    }

    @Test
    void testDeleteCard_Success() {
        // Arrange
        Long accountId = 1L;
        Long cardId = 10L;
        Long authenticatedUserId = 100L;

        AccountResponse accountResponse = AccountResponse.builder()
                .id(accountId)
                .userId(authenticatedUserId)
                .build();

        CardResponse deletedCardResponse = CardResponse.builder()
                .id(cardId)
                .accountId(accountId)
                .lastFourDigits("3456")
                .maskedNumber("**** **** **** 3456")
                .build();

        when(accountService.getAccountById(accountId)).thenReturn(accountResponse);
        when(cardService.deleteCard(accountId, cardId)).thenReturn(deletedCardResponse);

        // Act
        ResponseEntity<CardResponse> response = cardController.deleteCard(accountId, cardId, authenticatedUserId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardId, response.getBody().getId());
        verify(accountService, times(1)).getAccountById(accountId);
        verify(cardService, times(1)).deleteCard(accountId, cardId);
    }
}
