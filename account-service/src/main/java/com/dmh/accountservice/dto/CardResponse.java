package com.dmh.accountservice.dto;

import com.dmh.accountservice.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de tarjetas.
 * IMPORTANTE: Solo expone datos seguros (últimos 4 dígitos, nunca número completo ni CVV)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {

    private Long id;
    private Long accountId;
    
    /**
     * Solo los últimos 4 dígitos para seguridad PCI-DSS.
     * Ej: "1234"
     */
    private String lastFourDigits;
    
    /**
     * Número de tarjeta enmascarado para UI.
     * Ej: "**** **** **** 1234"
     */
    private String maskedNumber;
    
    private String cardHolderName;
    private LocalDate expirationDate;
    private Card.CardType cardType;
    private Card.CardBrand cardBrand;
    private Card.CardStatus status;
    private LocalDateTime createdAt;

    /**
     * Método helper para construir número enmascarado.
     */
    public static CardResponse fromEntity(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .accountId(card.getAccount().getId())
                .lastFourDigits(card.getLastFourDigits())
                .maskedNumber("**** **** **** " + card.getLastFourDigits())
                .cardHolderName(card.getCardHolderName())
                .expirationDate(card.getExpirationDate())
                .cardType(card.getCardType())
                .cardBrand(card.getCardBrand())
                .status(card.getStatus())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
