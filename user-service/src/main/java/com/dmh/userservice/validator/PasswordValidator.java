package com.dmh.userservice.validator;

import com.dmh.userservice.exception.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#-])[A-Za-z\\d@$!%*?&_#-]{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("Password cannot be null or empty");
        }

        if (!pattern.matcher(password).matches()) {
            throw new InvalidPasswordException(
                    "Password must be at least 8 characters and contain: " +
                            "1 uppercase letter, 1 lowercase letter, 1 number, " +
                            "and 1 special character (@$!%*?&_#-)");
        }
    }
}
