package com.dmh.userservice.controller;

import com.dmh.userservice.dto.LoginRequest;
import com.dmh.userservice.dto.LoginResponse;
import com.dmh.userservice.dto.LogoutResponse;
import com.dmh.userservice.dto.RegisterUserRequest;
import com.dmh.userservice.dto.UserResponse;
import com.dmh.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        logger.info("POST /api/users/register - Registering user with email: {}", request.getEmail());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("POST /api/users/login - Login attempt for email: {}", request.getEmail());
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/users/logout - Logout attempt");

        String token = extractTokenFromHeader(authHeader);

        LogoutResponse response = userService.logout(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam("token") String token) {
        logger.debug("GET /api/users/token/validate - Validating token");
        boolean isValid = userService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }
        return authHeader.substring(7);
    }
}
