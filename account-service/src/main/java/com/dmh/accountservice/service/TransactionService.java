package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.ActivityFilterRequest;
import com.dmh.accountservice.dto.AmountRange;
import com.dmh.accountservice.dto.CreateDepositRequest;
import com.dmh.accountservice.dto.CreateTransferRequest;
import com.dmh.accountservice.dto.DepositResponse;
import com.dmh.accountservice.dto.RecentTransferRecipient;
import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.dto.TransferResponse;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Card;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.exception.CardNotFoundException;
import com.dmh.accountservice.exception.ForbiddenAccessException;
import com.dmh.accountservice.exception.InsufficientFundsException;
import com.dmh.accountservice.exception.TransactionNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.CardRepository;
import com.dmh.accountservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CardRepository cardRepository,
                              AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getLastTransactions(Long accountId, Integer limit) {
        logger.info("Fetching last {} transactions for accountId: {}", limit, accountId);

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

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllActivity(Long accountId, Long requestingUserId) {
        return getAllActivity(accountId, requestingUserId, null);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllActivity(Long accountId, Long requestingUserId, ActivityFilterRequest filters) {
        logger.info("Fetching activity for accountId: {}, requestingUserId: {}, filters: {}", 
                    accountId, requestingUserId, filters);

        // Validar que la cuenta exista
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // Validar que el usuario sea el dueño de la cuenta
        if (!account.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to access activity of account {} owned by user {}",
                    requestingUserId, accountId, account.getUserId());
            throw new ForbiddenAccessException("You do not have permission to access this account's activity");
        }

        // Obtener todas las transacciones
        List<Transaction> transactions = transactionRepository
                .findAllByAccountIdOrderByCreatedAtDesc(accountId);

        // Aplicar filtros si se proporcionaron
        if (filters != null) {
            transactions = applyFilters(transactions, filters);
        }

        logger.info("Found {} transactions for accountId: {} (after filters)", transactions.size(), accountId);

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    private List<Transaction> applyFilters(List<Transaction> transactions, ActivityFilterRequest filters) {
        return transactions.stream()
                .filter(t -> applyTypeFilter(t, filters.getType()))
                .filter(t -> applyAmountRangeFilter(t, filters.getAmountRange()))
                .filter(t -> applyDateFilter(t, filters.getDateFrom(), filters.getDateTo()))
                .collect(Collectors.toList());
    }


    private boolean applyTypeFilter(Transaction transaction, Transaction.TransactionType type) {
        if (type == null) {
            return true; // Sin filtro
        }
        return transaction.getType() == type;
    }

    private boolean applyAmountRangeFilter(Transaction transaction, AmountRange range) {
        if (range == null) {
            return true; // Sin filtro
        }
        return range.contains(transaction.getAmount());
    }

    private boolean applyDateFilter(Transaction transaction, java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return true; // Sin filtro
        }

        java.time.LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();

        if (dateFrom != null && transactionDate.isBefore(dateFrom)) {
            return false;
        }

        if (dateTo != null && transactionDate.isAfter(dateTo)) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getActivityDetail(Long accountId, Long transactionId, Long requestingUserId) {
        logger.info("Fetching activity detail for accountId: {}, transactionId: {}, requestingUserId: {}", 
                    accountId, transactionId, requestingUserId);

        // Validar que la cuenta exista
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // Validar que el usuario sea el dueño de la cuenta
        if (!account.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to access transaction {} of account {} owned by user {}",
                    requestingUserId, transactionId, accountId, account.getUserId());
            throw new ForbiddenAccessException("You do not have permission to access this account's activity");
        }

        // Buscar la transacción específica
        Transaction transaction = transactionRepository.findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(() -> {
                    logger.warn("Transaction not found: transactionId={}, accountId={}", transactionId, accountId);
                    return new TransactionNotFoundException(
                            "Transaction not found with ID: " + transactionId + " for account: " + accountId);
                });

        logger.info("Transaction found: ID={}, Type={}, Amount={}", 
                    transaction.getId(), transaction.getType(), transaction.getAmount());

        return mapToResponse(transaction);
    }

    @Transactional
    public DepositResponse createDeposit(Long accountId, CreateDepositRequest request, Long requestingUserId) {
        logger.info("Creating deposit for accountId: {}, cardId: {}, amount: {}, requestingUserId: {}", 
                    accountId, request.getCardId(), request.getAmount(), requestingUserId);

        // Validar que la cuenta exista
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // Validar que el usuario sea el dueño de la cuenta
        if (!account.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to deposit to account {} owned by user {}",
                    requestingUserId, accountId, account.getUserId());
            throw new ForbiddenAccessException("You do not have permission to deposit to this account");
        }

        // Validar que la tarjeta exista y pertenezca a la cuenta
        Card card = cardRepository.findByIdAndAccountId(request.getCardId(), accountId)
                .orElseThrow(() -> {
                    logger.warn("Card not found: cardId={}, accountId={}", request.getCardId(), accountId);
                    return new CardNotFoundException(
                            "Card not found with ID: " + request.getCardId() + " for account: " + accountId);
                });

        // Validar que la tarjeta esté activa
        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            logger.warn("Attempted deposit with non-active card: cardId={}, status={}", 
                        request.getCardId(), card.getStatus());
            throw new IllegalArgumentException("Card is not active. Status: " + card.getStatus());
        }

        // Crear la transacción
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null ? 
                request.getDescription() : "Deposit from card **** " + card.getLastFourDigits());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction created: ID={}, Type={}, Amount={}", 
                    savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAmount());

        // Actualizar balance de la cuenta
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        accountService.updateBalance(accountId, newBalance);
        
        logger.info("Deposit completed successfully. New balance: {}", newBalance);

        return DepositResponse.builder()
                .transactionId(savedTransaction.getId())
                .accountId(accountId)
                .cardId(request.getCardId())
                .amount(request.getAmount())
                .description(savedTransaction.getDescription())
                .status(savedTransaction.getStatus().name())
                .newBalance(newBalance)
                .createdAt(savedTransaction.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecentTransferRecipient> getRecentTransfers(Long accountId, Integer limit, Long requestingUserId) {
        logger.info("Fetching recent transfers for accountId: {}, requestingUserId: {}, limit: {}", 
                    accountId, requestingUserId, limit);

        // Validar que la cuenta exista
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // Validar que el usuario sea el dueño de la cuenta
        if (!account.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to access transfers of account {} owned by user {}",
                    requestingUserId, accountId, account.getUserId());
            throw new ForbiddenAccessException("You do not have permission to access this account's transfers");
        }

        int finalLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
        Pageable pageable = PageRequest.of(0, finalLimit);

        // Obtener solo transferencias salientes (TRANSFER_OUT)
        List<Transaction> transfersOut = transactionRepository
                .findTransfersOutByAccountId(accountId, pageable);

        // Agrupar por destinatario y obtener la última fecha de transferencia
        // Extraemos el destinatario de la descripción: "Transfer to CVU: xxxxxxx" o "Transfer to Alias: xxxxxx"
        List<RecentTransferRecipient> recipients = transfersOut.stream()
                .map(t -> extractRecipientFromDescription(t.getDescription()))
                .collect(Collectors.toList());

        logger.info("Found {} recent transfer recipients for accountId: {}", recipients.size(), accountId);

        return recipients;
    }

    @Transactional
    public TransferResponse performTransfer(Long accountId, CreateTransferRequest request, Long requestingUserId) {
        logger.info("Performing transfer from accountId: {}, destination: {}, amount: {}, requestingUserId: {}", 
                    accountId, request.getDestination(), request.getAmount(), requestingUserId);

        // Validar que la cuenta origen exista
        Account sourceAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found with ID: " + accountId));

        // Validar que el usuario sea el dueño de la cuenta origen
        if (!sourceAccount.getUserId().equals(requestingUserId)) {
            logger.warn("User {} attempted to transfer from account {} owned by user {}",
                    requestingUserId, accountId, sourceAccount.getUserId());
            throw new ForbiddenAccessException("You do not have permission to transfer from this account");
        }

        // Validar fondos suficientes
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            logger.warn("Insufficient funds for transfer from accountId: {}. Balance: {}, Amount: {}",
                    accountId, sourceAccount.getBalance(), request.getAmount());
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + sourceAccount.getBalance());
        }

        // Buscar cuenta destino por CVU o alias
        String destination = request.getDestination();
        Account destinationAccount = accountRepository.findByCvuOrAlias(destination, destination)
                .orElseThrow(() -> {
                    logger.warn("Destination account not found: {}", destination);
                    return new AccountNotFoundException(
                            "Destination account not found with CVU or alias: " + destination);
                });

        // Validar que no sea transferencia a sí mismo
        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            logger.warn("Attempted transfer to same account: accountId={}", accountId);
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Crear transacción de salida (TRANSFER_OUT)
        Transaction transferOut = new Transaction();
        transferOut.setAccount(sourceAccount);
        transferOut.setType(Transaction.TransactionType.TRANSFER_OUT);
        transferOut.setAmount(request.getAmount());
        transferOut.setDescription(request.getDescription() != null ? 
                request.getDescription() : "Transfer to " + formatDestination(destination));
        transferOut.setStatus(Transaction.TransactionStatus.COMPLETED);

        // Crear transacción de entrada (TRANSFER_IN)
        Transaction transferIn = new Transaction();
        transferIn.setAccount(destinationAccount);
        transferIn.setType(Transaction.TransactionType.TRANSFER_IN);
        transferIn.setAmount(request.getAmount());
        transferIn.setDescription(request.getDescription() != null ? 
                request.getDescription() : "Transfer from " + sourceAccount.getCvu());
        transferIn.setStatus(Transaction.TransactionStatus.COMPLETED);

        // Guardar ambas transacciones
        Transaction savedTransferOut = transactionRepository.save(transferOut);
        Transaction savedTransferIn = transactionRepository.save(transferIn);

        logger.info("Transactions created: transferOut ID={}, transferIn ID={}", 
                    savedTransferOut.getId(), savedTransferIn.getId());

        // Actualizar balances (ATOMICIDAD: ambas actualizaciones en una transacción)
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(request.getAmount());
        accountService.updateBalance(accountId, newSourceBalance);

        BigDecimal newDestinationBalance = destinationAccount.getBalance().add(request.getAmount());
        accountService.updateBalance(destinationAccount.getId(), newDestinationBalance);

        logger.info("Transfer completed successfully. Source new balance: {}, Destination new balance: {}", 
                   newSourceBalance, newDestinationBalance);

        return TransferResponse.builder()
                .transactionId(savedTransferOut.getId())
                .accountId(accountId)
                .destination(destination)
                .amount(request.getAmount())
                .description(savedTransferOut.getDescription())
                .status(savedTransferOut.getStatus().name())
                .newBalance(newSourceBalance)
                .createdAt(savedTransferOut.getCreatedAt())
                .build();
    }

    /**
     * Extrae el destinatario de la descripción de la transacción.
     * Formato esperado: "Transfer to CVU: xxxxxxx" o "Transfer to Alias: xxxxxx"
     */
    private RecentTransferRecipient extractRecipientFromDescription(String description) {
        // El formato es "Transfer to CVU: xxxxxxx" o "Transfer to Alias: xxxxxx"
        String[] parts = description.split(": ");
        String destination = parts.length > 1 ? parts[1] : description;

        return RecentTransferRecipient.builder()
                .destination(destination)
                .amount(null) // No tenemos el monto aquí, pero podríamos extraerlo si se cambia el formato
                .lastTransferDate(null) // Necesitaríamos la fecha de la transacción
                .build();
    }

    /**
     * Formatea el destino para mostrar en la descripción.
     * Determina si es CVU (22 dígitos) o alias (contiene puntos).
     */
    private String formatDestination(String destination) {
        if (destination.matches("\\d{22}")) {
            return "CVU: " + destination;
        } else if (destination.contains(".")) {
            return "Alias: " + destination;
        } else {
            return "CBU: " + destination;
        }
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
