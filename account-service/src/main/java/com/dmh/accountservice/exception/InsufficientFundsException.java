package com.dmh.accountservice.exception;

/**
 * Exception thrown when an account has insufficient funds to complete a transfer.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
