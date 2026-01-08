package com.dmh.userservice.service;

import com.dmh.userservice.client.AccountServiceClient;
import com.dmh.userservice.dto.AccountResponseDTO;
import com.dmh.userservice.dto.LoginRequest;
import com.dmh.userservice.dto.LoginResponse;
import com.dmh.userservice.dto.RegisterUserRequest;
import com.dmh.userservice.entity.TokenBlacklist;
import com.dmh.userservice.entity.User;
import com.dmh.userservice.exception.InvalidCredentialsException;
import com.dmh.userservice.exception.UserAlreadyExistsException;
import com.dmh.userservice.repository.TokenBlacklistRepository;
import com.dmh.userservice.repository.UserRepository;
import com.dmh.userservice.util.JwtUtil;
import com.dmh.userservice.validator.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository,
            accountServiceClient,
            passwordValidator,
            jwtUtil,
            tokenBlacklistRepository
        );
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void testRegisterUser_Success() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Juan");
        request.setLastName("Perez");
        request.setDni("12345678");
        request.setEmail("juan@example.com");
        request.setPhone("5491155555555");
        request.setPassword("Password123@");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDni(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AccountResponseDTO accountResponse = new AccountResponseDTO();
        accountResponse.setId(1L);
        accountResponse.setCvu("1234567890123456789012");
        accountResponse.setAlias("sol.luna.estrella");

        when(accountServiceClient.createAccount(any())).thenReturn(ResponseEntity.ok(accountResponse));

        assertDoesNotThrow(() -> userService.registerUser(request));
        verify(userRepository, times(1)).save(any(User.class));
        verify(accountServiceClient, times(1)).createAccount(any());
    }

    @Test
    void testRegisterUser_DniAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Juan");
        request.setDni("12345678");
        request.setEmail("juan@example.com");
        request.setPassword("Password123@");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDni(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(request));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFirstName("Juan");
        request.setDni("12345678");
        request.setEmail("juan@example.com");
        request.setPassword("Password123@");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(request));
    }

    @Test
    void testLoginUser_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@example.com");
        request.setPassword("Password123@");

        User user = new User();
        user.setId(1L);
        user.setEmail("juan@example.com");
        user.setPassword("hashedPassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("jwt.token.here");

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("juan@example.com", response.getEmail());
        assertNotNull(response.getToken());
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@example.com");
        request.setPassword("WrongPassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void testLogoutUser_Success() {
        String token = "valid.token.here";
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUserId(anyString())).thenReturn(1L);
        when(jwtUtil.extractExpiration(anyString())).thenReturn(new java.util.Date());
        when(tokenBlacklistRepository.existsByToken(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> userService.logout(token));
        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    @Test
    void testIsTokenValid_Valid() {
        String token = "valid.token.here";
        when(tokenBlacklistRepository.existsByToken(anyString())).thenReturn(false);

        boolean isValid = userService.isTokenValid(token);

        assertTrue(isValid);
    }

    @Test
    void testIsTokenValid_Blacklisted() {
        String token = "blacklisted.token";
        when(tokenBlacklistRepository.existsByToken(anyString())).thenReturn(true);

        boolean isValid = userService.isTokenValid(token);

        assertFalse(isValid);
    }
}
