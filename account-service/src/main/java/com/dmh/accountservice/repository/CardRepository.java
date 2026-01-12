package com.dmh.accountservice.repository;

import com.dmh.accountservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Encuentra todas las tarjetas asociadas a una cuenta específica.
     * @param accountId ID de la cuenta
     * @return Lista de tarjetas (puede estar vacía)
     */
    @Query("SELECT c FROM Card c WHERE c.account.id = :accountId")
    List<Card> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Encuentra una tarjeta específica que pertenezca a una cuenta.
     * @param cardId ID de la tarjeta
     * @param accountId ID de la cuenta
     * @return Optional con la tarjeta si existe y pertenece a la cuenta
     */
    @Query("SELECT c FROM Card c WHERE c.id = :cardId AND c.account.id = :accountId")
    Optional<Card> findByIdAndAccountId(@Param("cardId") Long cardId, @Param("accountId") Long accountId);

    /**
     * Verifica si existe una tarjeta con los últimos 4 dígitos para una cuenta.
     * Útil para evitar duplicados.
     */
    boolean existsByLastFourDigitsAndAccountId(String lastFourDigits, Long accountId);
}
