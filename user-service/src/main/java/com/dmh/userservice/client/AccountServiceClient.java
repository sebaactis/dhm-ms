package com.dmh.userservice.client;

import com.dmh.userservice.dto.AccountResponseDTO;
import com.dmh.userservice.dto.CreateAccountRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PostMapping("/api/accounts")
    ResponseEntity<AccountResponseDTO> createAccount(@RequestBody CreateAccountRequestDTO request);

    @GetMapping("/api/accounts/user/{userId}")
    ResponseEntity<AccountResponseDTO> getAccountByUserId(@PathVariable Long userId);
}
