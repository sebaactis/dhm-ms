package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.ActivityFilterRequest;
import com.dmh.accountservice.dto.AmountRange;
import com.dmh.accountservice.dto.CreateDepositRequest;
import com.dmh.accountservice.dto.DepositResponse;
import com.dmh.accountservice.dto.TransactionResponse;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.entity.Card;
import com.dmh.accountservice.entity.Transaction;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.exception.CardNotFoundException;
import com.dmh.accountservice.exception.ForbiddenAccessException;
import com.dmh.accountservice.exception.TransactionNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import com.dmh.accountservice.repository.CardRepository;
import com.dmh.accountservice.repository.TransactionRepository;
import com.dmh.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Transaction testTransaction1;
    private Transaction testTransaction2;
    private Transaction testTransaction3;
    private Transaction testTransaction4;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUserId(100L);
        testAccount.setCvu("1234567890123456789012");
        testAccount.setAlias("test.alias.uno");
        testAccount.setBalance(BigDecimal.valueOf(1000.00));

        testTransaction1 = new Transaction();
        testTransaction1.setId(1L);
        testTransaction1.setAccount(testAccount);
        testTransaction1.setType(Transaction.TransactionType.DEPOSIT);
        testTransaction1.setAmount(BigDecimal.valueOf(500.00));
        testTransaction1.setDescription("Test deposit");
        testTransaction1.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction1.setCreatedAt(LocalDateTime.now().minusDays(1));

        testTransaction2 = new Transaction();
        testTransaction2.setId(2L);
        testTransaction2.setAccount(testAccount);
        testTransaction2.setType(Transaction.TransactionType.WITHDRAWAL);
        testTransaction2.setAmount(BigDecimal.valueOf(200.00));
        testTransaction2.setDescription("Test withdrawal");
        testTransaction2.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction2.setCreatedAt(LocalDateTime.now());

        testCard = new Card();
        testCard.setId(1L);
        testCard.setAccount(testAccount);
        testCard.setLastFourDigits("1234");
        testCard.setCardHolderName("Test User");
        testCard.setExpirationDate(java.time.LocalDate.now().plusYears(2));
        testCard.setCardType(Card.CardType.DEBIT);
        testCard.setCardBrand(Card.CardBrand.VISA);
        testCard.setStatus(Card.CardStatus.ACTIVE);

        // Transacciones adicionales para probar filtros
        testTransaction3 = new Transaction();
        testTransaction3.setId(3L);
        testTransaction3.setAccount(testAccount);
        testTransaction3.setType(Transaction.TransactionType.DEPOSIT);
        testTransaction3.setAmount(BigDecimal.valueOf(3000.00));
        testTransaction3.setDescription("Large deposit");
        testTransaction3.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction3.setCreatedAt(LocalDateTime.now().minusDays(5));

        testTransaction4 = new Transaction();
        testTransaction4.setId(4L);
        testTransaction4.setAccount(testAccount);
        testTransaction4.setType(Transaction.TransactionType.WITHDRAWAL);
        testTransaction4.setAmount(BigDecimal.valueOf(15000.00));
        testTransaction4.setDescription("Large withdrawal");
        testTransaction4.setStatus(Transaction.TransactionStatus.COMPLETED);
        testTransaction4.setCreatedAt(LocalDateTime.now().minusDays(10));
    }

    @Test
    @DisplayName("Should return last N transactions when account exists")
    void shouldReturnLastTransactionsWhenAccountExists() {
        // Given
        Long accountId = 1L;
        Integer limit = 5;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(Arrays.asList(testTransaction2, testTransaction1));

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, limit);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountId()).isEqualTo(accountId);
        assertThat(result.get(0).getType()).isEqualTo("WITHDRAWAL");
        assertThat(result.get(1).getType()).isEqualTo("DEPOSIT");
    }

    @Test
    @DisplayName("Should use default limit when null is provided")
    void shouldUseDefaultLimitWhenNullProvided() {
        // Given
        Long accountId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), eq(PageRequest.of(0, 5))))
                .thenReturn(List.of(testTransaction1));

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, null);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        Long accountId = 999L;
        when(accountRepository.existsById(accountId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> transactionService.getLastTransactions(accountId, 5))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactions() {
        // Given
        Long accountId = 1L;
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findLastTransactionsByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(List.of());

        // When
        List<TransactionResponse> result = transactionService.getLastTransactions(accountId, 5);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return all activity when user owns the account")
    void shouldReturnAllActivityWhenUserOwnsAccount() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction2, testTransaction1));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("WITHDRAWAL");
        assertThat(result.get(1).getType()).isEqualTo("DEPOSIT");
    }

    @Test
    @DisplayName("Should throw ForbiddenAccessException when user does not own the account")
    void shouldThrowForbiddenAccessExceptionWhenUserDoesNotOwnAccount() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 999L; // Different from testAccount.userId (100L)
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> transactionService.getAllActivity(accountId, requestingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("You do not have permission to access this account's activity");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist for getAllActivity")
    void shouldThrowAccountNotFoundExceptionForGetAllActivity() {
        // Given
        Long accountId = 999L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getAllActivity(accountId, requestingUserId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
    }

    @Test
    @DisplayName("Should return empty list when account has no transactions")
    void shouldReturnEmptyListWhenAccountHasNoTransactions() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(List.of());

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return activity detail when user owns the account and transaction exists")
    void shouldReturnActivityDetailWhenUserOwnsAccountAndTransactionExists() {
        // Given
        Long accountId = 1L;
        Long transactionId = 1L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(java.util.Optional.of(testTransaction1));

        // When
        TransactionResponse result = transactionService.getActivityDetail(accountId, transactionId, requestingUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("DEPOSIT");
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
    }

    @Test
    @DisplayName("Should throw ForbiddenAccessException when user does not own the account for activity detail")
    void shouldThrowForbiddenAccessExceptionWhenUserDoesNotOwnAccountForActivityDetail() {
        // Given
        Long accountId = 1L;
        Long transactionId = 1L;
        Long requestingUserId = 999L; // Different from testAccount.userId (100L)
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> transactionService.getActivityDetail(accountId, transactionId, requestingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("You do not have permission to access this account's activity");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist for activity detail")
    void shouldThrowAccountNotFoundExceptionForActivityDetail() {
        // Given
        Long accountId = 999L;
        Long transactionId = 1L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getActivityDetail(accountId, transactionId, requestingUserId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction does not exist")
    void shouldThrowTransactionNotFoundExceptionWhenTransactionDoesNotExist() {
        // Given
        Long accountId = 1L;
        Long transactionId = 999L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getActivityDetail(accountId, transactionId, requestingUserId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction belongs to different account")
    void shouldThrowTransactionNotFoundExceptionWhenTransactionBelongsToDifferentAccount() {
        // Given
        Long accountId = 1L;
        Long transactionId = 1L;
        Long requestingUserId = 100L;
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(java.util.Optional.empty()); // Transaction not found for this account

        // When & Then
        assertThatThrownBy(() -> transactionService.getActivityDetail(accountId, transactionId, requestingUserId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction not found with ID: 1 for account: 1");
    }

    @Test
    @DisplayName("Should create deposit successfully when all validations pass")
    void shouldCreateDepositSuccessfully() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        CreateDepositRequest request = new CreateDepositRequest();
        request.setCardId(1L);
        request.setAmount(BigDecimal.valueOf(500.00));
        request.setDescription("Test deposit");

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(cardRepository.findByIdAndAccountId(1L, accountId)).thenReturn(java.util.Optional.of(testCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        // When
        DepositResponse response = transactionService.createDeposit(accountId, request, requestingUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(100L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        assertThat(response.getNewBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500.00)); // 1000 + 500
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountService, times(1)).updateBalance(eq(accountId), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenAccessException when user does not own account for deposit")
    void shouldThrowForbiddenAccessExceptionForDeposit() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 999L; // Different from testAccount.userId (100L)
        CreateDepositRequest request = new CreateDepositRequest();
        request.setCardId(1L);
        request.setAmount(BigDecimal.valueOf(500.00));

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> transactionService.createDeposit(accountId, request, requestingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("You do not have permission to deposit to this account");
    }

    @Test
    @DisplayName("Should throw CardNotFoundException when card does not exist")
    void shouldThrowCardNotFoundExceptionForDeposit() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        CreateDepositRequest request = new CreateDepositRequest();
        request.setCardId(999L);
        request.setAmount(BigDecimal.valueOf(500.00));

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(cardRepository.findByIdAndAccountId(999L, accountId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.createDeposit(accountId, request, requestingUserId))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when card is not active")
    void shouldThrowIllegalArgumentExceptionWhenCardNotActive() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        CreateDepositRequest request = new CreateDepositRequest();
        request.setCardId(1L);
        request.setAmount(BigDecimal.valueOf(500.00));

        Card blockedCard = new Card();
        blockedCard.setId(1L);
        blockedCard.setAccount(testAccount);
        blockedCard.setLastFourDigits("1234");
        blockedCard.setStatus(Card.CardStatus.BLOCKED);

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(cardRepository.findByIdAndAccountId(1L, accountId)).thenReturn(java.util.Optional.of(blockedCard));

        // When & Then
        assertThatThrownBy(() -> transactionService.createDeposit(accountId, request, requestingUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card is not active");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException for deposit when account does not exist")
    void shouldThrowAccountNotFoundExceptionForDeposit() {
        // Given
        Long accountId = 999L;
        Long requestingUserId = 100L;
        CreateDepositRequest request = new CreateDepositRequest();
        request.setCardId(1L);
        request.setAmount(BigDecimal.valueOf(500.00));

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.createDeposit(accountId, request, requestingUserId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with ID: 999");
    }

    @Test
    @DisplayName("Should filter activity by transaction type")
    void shouldFilterActivityByType() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        ActivityFilterRequest filters = ActivityFilterRequest.builder()
                .type(Transaction.TransactionType.DEPOSIT)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction1, testTransaction2, testTransaction3, testTransaction4));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId, filters);

        // Then
        assertThat(result).hasSize(2); // Solo DEPOSIT (testTransaction1 y testTransaction3)
        assertThat(result.get(0).getType()).isEqualTo("DEPOSIT");
        assertThat(result.get(1).getType()).isEqualTo("DEPOSIT");
    }

    @Test
    @DisplayName("Should filter activity by amount range")
    void shouldFilterActivityByAmountRange() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        ActivityFilterRequest filters = ActivityFilterRequest.builder()
                .amountRange(AmountRange.RANGE_1000_5000)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction1, testTransaction2, testTransaction3, testTransaction4));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId, filters);

        // Then
        assertThat(result).hasSize(1); // Solo testTransaction3 (3000)
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000.00));
    }

    @Test
    @DisplayName("Should filter activity by date range")
    void shouldFilterActivityByDateRange() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        java.time.LocalDate dateFrom = java.time.LocalDate.now().minusDays(7);
        java.time.LocalDate dateTo = java.time.LocalDate.now().minusDays(3);
        
        ActivityFilterRequest filters = ActivityFilterRequest.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction1, testTransaction2, testTransaction3, testTransaction4));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId, filters);

        // Then
        assertThat(result).hasSize(1); // Solo testTransaction3 (minusDays 5)
    }

    @Test
    @DisplayName("Should filter activity with multiple filters combined")
    void shouldFilterActivityWithMultipleFilters() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;
        ActivityFilterRequest filters = ActivityFilterRequest.builder()
                .type(Transaction.TransactionType.DEPOSIT)
                .amountRange(AmountRange.RANGE_1000_5000)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction1, testTransaction2, testTransaction3, testTransaction4));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId, filters);

        // Then
        assertThat(result).hasSize(1); // Solo testTransaction3 (DEPOSIT + 3000 en rango)
        assertThat(result.get(0).getType()).isEqualTo("DEPOSIT");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000.00));
    }

    @Test
    @DisplayName("Should return all activity when no filters are applied")
    void shouldReturnAllActivityWithoutFilters() {
        // Given
        Long accountId = 1L;
        Long requestingUserId = 100L;

        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(testAccount));
        when(transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Arrays.asList(testTransaction1, testTransaction2, testTransaction3, testTransaction4));

        // When
        List<TransactionResponse> result = transactionService.getAllActivity(accountId, requestingUserId, null);

        // Then
        assertThat(result).hasSize(4); // Todas las transacciones
    }
}
