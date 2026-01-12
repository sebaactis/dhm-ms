package com.dmh.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @NotNull(message = "Alias cannot be null")
    @NotBlank(message = "Alias cannot be empty")
    @Pattern(regexp = "^[a-z0-9]+\\.[a-z0-9]+\\.[a-z0-9]+$", 
             message = "Alias must follow format: word.word.word (lowercase alphanumeric)")
    private String alias;
}
