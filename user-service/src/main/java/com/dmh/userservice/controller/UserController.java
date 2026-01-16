package com.dmh.userservice.controller;

import com.dmh.userservice.dto.LoginRequest;
import com.dmh.userservice.dto.LoginResponse;
import com.dmh.userservice.dto.LogoutResponse;
import com.dmh.userservice.dto.RegisterUserRequest;
import com.dmh.userservice.dto.UpdateUserRequest;
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
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("POST /api/users/logout - Logout attempt for user: {}", authenticatedUserId);

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

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("GET /api/users/{} - Fetching user", id);

        // Validar que el usuario solo puede ver su propio perfil
        if (!authenticatedUserId.equals(id)) {
            logger.warn("User {} attempted to access profile of user {}", authenticatedUserId, id);
            throw new IllegalArgumentException("You can only access your own profile");
        }

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        logger.info("PATCH /api/users/{} - Updating user", id);

        // Validar que el usuario solo puede actualizar su propio perfil
        if (!authenticatedUserId.equals(id)) {
            logger.warn("User {} attempted to update profile of user {}", authenticatedUserId, id);
            throw new IllegalArgumentException("You can only update your own profile");
        }

        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }
        return authHeader.substring(7);
    }
}
