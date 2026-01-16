package com.dmh.userservice.service;

import com.dmh.userservice.client.AccountServiceClient;
import com.dmh.userservice.dto.AccountResponseDTO;
import com.dmh.userservice.dto.CreateAccountRequestDTO;
import com.dmh.userservice.dto.LoginRequest;
import com.dmh.userservice.dto.LoginResponse;
import com.dmh.userservice.dto.LogoutResponse;
import com.dmh.userservice.dto.RegisterUserRequest;
import com.dmh.userservice.dto.UserResponse;
import com.dmh.userservice.entity.TokenBlacklist;
import com.dmh.userservice.entity.User;
import com.dmh.userservice.exception.InvalidCredentialsException;
import com.dmh.userservice.exception.UserAlreadyExistsException;
import com.dmh.userservice.repository.TokenBlacklistRepository;
import com.dmh.userservice.repository.UserRepository;
import com.dmh.userservice.util.JwtUtil;
import com.dmh.userservice.validator.PasswordValidator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AccountServiceClient accountServiceClient;
    private final PasswordValidator passwordValidator;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public UserService(UserRepository userRepository,
                       AccountServiceClient accountServiceClient,
                       PasswordValidator passwordValidator,
                       JwtUtil jwtUtil,
                       TokenBlacklistRepository tokenBlacklistRepository) {
        this.userRepository = userRepository;
        this.accountServiceClient = accountServiceClient;
        this.passwordValidator = passwordValidator;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {
        logger.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByDni(request.getDni())) {
            throw new UserAlreadyExistsException("User with DNI " + request.getDni() + " already exists");
        }

        passwordValidator.validate(request.getPassword());

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDni(request.getDni());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(hashedPassword);

        User savedUser = userRepository.save(user);
        logger.info("User created with ID: {}", savedUser.getId());


        CreateAccountRequestDTO accountRequest = new CreateAccountRequestDTO(savedUser.getId());
        ResponseEntity<AccountResponseDTO> accountResponse = accountServiceClient.createAccount(accountRequest, savedUser.getId());
        AccountResponseDTO account = accountResponse.getBody();

        logger.info("Account created for userId: {}, CVU: {}, Alias: {}",
                savedUser.getId(), account.getCvu(), account.getAlias());

        return UserResponse.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .dni(savedUser.getDni())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed: User not found with email: {}", request.getEmail());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed: Invalid password for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        logger.info("Login successful for user ID: {}", user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .expiresIn(3600000L)
                .build();
    }


    @Transactional
    public LogoutResponse logout(String token) {
        logger.info("Logout attempt with token");

        if (!jwtUtil.validateToken(token)) {
            logger.warn("Logout failed: Invalid or expired token");
            throw new InvalidCredentialsException("Invalid or expired token");
        }


        Long userId = jwtUtil.extractUserId(token);
        Date expirationDate = jwtUtil.extractExpiration(token);

        LocalDateTime expiresAt = expirationDate.toInstant()
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();


        if (tokenBlacklistRepository.existsByToken(token)) {
            logger.info("Token already in blacklist for user ID: {}", userId);
            return LogoutResponse.builder()
                    .message("Logout successful")
                    .userId(userId)
                    .build();
        }

        TokenBlacklist blacklistEntry = new TokenBlacklist();
        blacklistEntry.setToken(token);
        blacklistEntry.setUserId(userId);
        blacklistEntry.setExpiresAt(expiresAt);

        tokenBlacklistRepository.save(blacklistEntry);
        logger.info("Token added to blacklist for user ID: {}", userId);

        return LogoutResponse.builder()
                .message("Logout successful")
                .userId(userId)
                .build();
    }

    public boolean isTokenValid(String token) {
        return !tokenBlacklistRepository.existsByToken(token);
    }

    /**
     * Obtiene un usuario por ID incluyendo datos de su cuenta
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        logger.info("Fetching user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new InvalidCredentialsException("User not found with ID: " + userId);
                });

        // Obtener datos de la cuenta
        ResponseEntity<AccountResponseDTO> accountResponse = accountServiceClient.getAccountByUserId(userId, userId);
        AccountResponseDTO account = accountResponse.getBody();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dni(user.getDni())
                .email(user.getEmail())
                .phone(user.getPhone())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .build();
    }

    /**
     * Actualiza email y/o phone de un usuario
     */
    @Transactional
    public UserResponse updateUser(Long userId, com.dmh.userservice.dto.UpdateUserRequest request) {
        logger.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new InvalidCredentialsException("User not found with ID: " + userId);
                });

        // Actualizar solo los campos que vienen en el request (PATCH semántico)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Validar que el nuevo email no esté en uso por otro usuario
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email " + request.getEmail() + " is already in use");
            }
            user.setEmail(request.getEmail());
            logger.info("Updated email for user ID: {}", userId);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
            logger.info("Updated phone for user ID: {}", userId);
        }

        User updatedUser = userRepository.save(user);

        // Obtener datos de la cuenta para incluir en la respuesta
        ResponseEntity<AccountResponseDTO> accountResponse = accountServiceClient.getAccountByUserId(userId, userId);
        AccountResponseDTO account = accountResponse.getBody();

        return UserResponse.builder()
                .id(updatedUser.getId())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .dni(updatedUser.getDni())
                .email(updatedUser.getEmail())
                .phone(updatedUser.getPhone())
                .cvu(account.getCvu())
                .alias(account.getAlias())
                .build();
    }
}
