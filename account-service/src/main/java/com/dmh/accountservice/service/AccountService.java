package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CreateAccountRequest;
import com.dmh.accountservice.dto.UpdateAccountRequest;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.exception.AccountAlreadyExistsException;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private static final int CVU_LENGTH = 22;
    private static final int MAX_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final List<String> words;
    private final Random random;

    public AccountService(AccountRepository accountRepository,
                          @Value("${account.alias.words-file:classpath:words.txt}") String wordsFile) {
        this.accountRepository = accountRepository;
        this.random = new SecureRandom();
        this.words = loadWords(wordsFile);
        logger.info("AccountService initialized with {} words for alias generation", words.size());
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        logger.info("Creating account for userId: {}", request.getUserId());

        if (accountRepository.existsByUserId(request.getUserId())) {
            throw new AccountAlreadyExistsException(
                    "Account already exists for user ID: " + request.getUserId());
        }

        String cvu = generateUniqueCvu();
        String alias = generateUniqueAlias();

        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setCvu(cvu);
        account.setAlias(alias);
        account.setBalance(BigDecimal.ZERO);

        Account savedAccount = accountRepository.save(account);
        logger.info("Account created successfully: CVU={}, Alias={}", cvu, alias);

        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserId(Long userId) {
        logger.info("Fetching account for userId: {}", userId);
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found for user ID: " + userId));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        logger.info("Fetching account for accountId: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId));
        return mapToResponse(account);
    }

    /**
     * Actualiza el alias de una cuenta
     */
    @Transactional
    public AccountResponse updateAccount(Long accountId, UpdateAccountRequest request) {
        logger.info("Updating account with ID: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId));

        // Validar que el nuevo alias no esté en uso
        if (!request.getAlias().equals(account.getAlias()) && 
            accountRepository.existsByAlias(request.getAlias())) {
            throw new AccountAlreadyExistsException("Alias " + request.getAlias() + " is already in use");
        }

        account.setAlias(request.getAlias());
        Account updatedAccount = accountRepository.save(account);
        logger.info("Account updated successfully: ID={}, New Alias={}", accountId, request.getAlias());

        return mapToResponse(updatedAccount);
    }

    /**
     * Genera un CVU único de 22 dígitos
     */
    private String generateUniqueCvu() {
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            String cvu = generateCvu();
            if (!accountRepository.existsByCvu(cvu)) {
                return cvu;
            }
            attempts++;
            logger.warn("CVU collision detected, attempt {}/{}", attempts, MAX_ATTEMPTS);
        }
        throw new RuntimeException("Failed to generate unique CVU after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Genera un CVU de 22 dígitos
     */
    private String generateCvu() {
        StringBuilder cvu = new StringBuilder(CVU_LENGTH);
        for (int i = 0; i < CVU_LENGTH; i++) {
            cvu.append(random.nextInt(10));
        }
        return cvu.toString();
    }

    /**
     * Genera un alias único de 3 palabras separadas por punto
     */
    private String generateUniqueAlias() {
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            String alias = generateAlias();
            if (!accountRepository.existsByAlias(alias)) {
                return alias;
            }
            attempts++;
            logger.warn("Alias collision detected, attempt {}/{}", attempts, MAX_ATTEMPTS);
        }
        throw new RuntimeException("Failed to generate unique alias after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Genera un alias de 3 palabras aleatorias del archivo words.txt
     */
    private String generateAlias() {
        String word1 = words.get(random.nextInt(words.size()));
        String word2 = words.get(random.nextInt(words.size()));
        String word3 = words.get(random.nextInt(words.size()));
        return word1 + "." + word2 + "." + word3;
    }

    /**
     * Carga las palabras desde el archivo words.txt
     */
    private List<String> loadWords(String wordsFile) {
        List<String> wordList = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("words.txt");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    wordList.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Error loading words file: {}", wordsFile, e);
            throw new RuntimeException("Failed to load words file for alias generation", e);
        }

        if (wordList.isEmpty()) {
            throw new RuntimeException("Words file is empty, cannot generate aliases");
        }

        return wordList;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
