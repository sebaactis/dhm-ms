package com.dmh.userservice.validator;

import com.dmh.userservice.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @InjectMocks
    private PasswordValidator passwordValidator;

    @Test
    void testValidPassword() {
        assertDoesNotThrow(() -> passwordValidator.validate("Password123@"));
    }

    @Test
    void testPasswordTooShort() {
        assertThrows(InvalidPasswordException.class,
            () -> passwordValidator.validate("Pass1@"));
    }

    @Test
    void testPasswordNoUppercase() {
        assertThrows(InvalidPasswordException.class,
            () -> passwordValidator.validate("password123@"));
    }

    @Test
    void testPasswordNoLowercase() {
        assertThrows(InvalidPasswordException.class,
            () -> passwordValidator.validate("PASSWORD123@"));
    }

    @Test
    void testPasswordNoNumber() {
        assertThrows(InvalidPasswordException.class,
            () -> passwordValidator.validate("Password@"));
    }

    @Test
    void testPasswordNoSpecialChar() {
        assertThrows(InvalidPasswordException.class,
            () -> passwordValidator.validate("Password123"));
    }
}
