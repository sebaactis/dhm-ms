package com.dmh.accountservice.service;

import com.dmh.accountservice.dto.AccountResponse;
import com.dmh.accountservice.dto.CreateAccountRequest;
import com.dmh.accountservice.entity.Account;
import com.dmh.accountservice.exception.AccountAlreadyExistsException;
import com.dmh.accountservice.exception.AccountNotFoundException;
import com.dmh.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void testCreateAccount_Success() {
        CreateAccountRequest request = new CreateAccountRequest(1L);

        when(accountRepository.existsByUserId(anyLong())).thenReturn(false);
        when(accountRepository.existsByCvu(anyString())).thenReturn(false);
        when(accountRepository.existsByAlias(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertNotNull(response.getCvu());
        assertTrue(response.getCvu().matches("\\d{22}"));
        assertNotNull(response.getAlias());
        assertTrue(response.getAlias().matches("[a-z]+\\.[a-z]+\\.[a-z]+"));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testCreateAccount_AlreadyExists() {
        CreateAccountRequest request = new CreateAccountRequest(1L);

        when(accountRepository.existsByUserId(anyLong())).thenReturn(true);

        assertThrows(AccountAlreadyExistsException.class, () -> accountService.createAccount(request));
    }

    @Test
    void testGetAccountByUserId_Success() {
        Long userId = 1L;
        Account account = new Account();
        account.setId(1L);
        account.setUserId(userId);
        account.setCvu("1234567890123456789012");
        account.setAlias("sol.luna.estrella");
        account.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByUserId(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("1234567890123456789012", response.getCvu());
        assertEquals("sol.luna.estrella", response.getAlias());
    }

    @Test
    void testGetAccountByUserId_NotFound() {
        Long userId = 1L;
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountByUserId(userId));
    }
}
