package com.dmh.userservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid number (10-15 digits)")
    private String phone;

    @AssertTrue(message = "At least one field (email or phone) must be provided")
    private boolean isValid() {
        return (email != null && !email.isBlank()) || 
               (phone != null && !phone.isBlank());
    }
}
