package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Obtiene las últimas N transacciones de una cuenta
     *
     * @param accountId ID de la cuenta
     * @param limit cantidad de transacciones a retornar (default: 5)
     * @return lista de TransactionResponse ordenadas por fecha descendente
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getLastTransactions(Long accountId, Integer limit) {
        logger.info("Fetching last {} transactions for accountId: {}", limit, accountId);

        // Validar que la cuenta exista
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found with ID: " + accountId);
        }

        int finalLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, finalLimit);

        List<Transaction> transactions = transactionRepository
                .findLastTransactionsByAccountId(accountId, pageable);

        logger.info("Found {} transactions for accountId: {}", transactions.size(), accountId);

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
