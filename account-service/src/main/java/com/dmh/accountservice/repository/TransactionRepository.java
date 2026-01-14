package com.dmh.accountservice.repository;

import com.dmh.accountservice.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Obtiene las últimas N transacciones de una cuenta ordenadas por fecha descendente
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findLastTransactionsByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Obtiene todas las transacciones de una cuenta ordenadas por fecha descendente (más reciente primero)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);

    /**
     * Busca una transacción específica por ID y accountId
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :transactionId AND t.account.id = :accountId")
    java.util.Optional<Transaction> findByIdAndAccountId(@Param("transactionId") Long transactionId, 
                                                           @Param("accountId") Long accountId);

    /**
     * Obtiene las últimas N transferencias salientes de una cuenta
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.type = 'TRANSFER_OUT' ORDER BY t.createdAt DESC")
    List<Transaction> findTransfersOutByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
