package com.dmh.accountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "card_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Column(name = "card_brand", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CardStatus.ACTIVE;
        }
    }

    public enum CardType {
        DEBIT,
        CREDIT
    }

    public enum CardBrand {
        VISA,
        MASTERCARD,
        AMEX,
        MAESTRO
    }

    public enum CardStatus {
        ACTIVE,
        BLOCKED,
        EXPIRED
    }
}
