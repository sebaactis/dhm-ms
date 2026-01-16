package com.dmh.userservice.controller;

import com.dmh.userservice.dto.LoginRequest;
import com.dmh.userservice.dto.LoginResponse;
import com.dmh.userservice.dto.RegisterUserRequest;
import com.dmh.userservice.dto.UserResponse;
import com.dmh.userservice.exception.InvalidPasswordException;
import com.dmh.userservice.exception.UserAlreadyExistsException;
import com.dmh.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.fail-fast=false"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void testRegister_Success() throws Exception {
        String requestJson = """
            {
                "firstName": "Juan",
                "lastName": "Perez",
                "dni": "12345678",
                "email": "juan@example.com",
                "phone": "5491155555555",
                "password": "Password123@"
            }
            """;

        when(userService.registerUser(any(RegisterUserRequest.class))).thenReturn(
            UserResponse.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Perez")
                .dni("12345678")
                .email("juan@example.com")
                .phone("5491155555555")
                .cvu("1234567890123456789012")
                .alias("sol.luna.estrella")
                .build()
        );

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("Juan"))
            .andExpect(jsonPath("$.email").value("juan@example.com"));
    }

    @Test
    void testRegister_DniAlreadyExists() throws Exception {
        String requestJson = """
            {
                "firstName": "Juan",
                "lastName": "Perez",
                "dni": "12345678",
                "email": "juan@example.com",
                "phone": "5491155555555",
                "password": "Password123@"
            }
            """;

        when(userService.registerUser(any(RegisterUserRequest.class)))
            .thenThrow(new UserAlreadyExistsException("User with DNI already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isConflict());
    }

    @Test
    void testRegister_InvalidPassword() throws Exception {
        String requestJson = """
            {
                "firstName": "Juan",
                "password": "weak"
            }
            """;

        when(userService.registerUser(any(RegisterUserRequest.class)))
            .thenThrow(new InvalidPasswordException("Password does not meet requirements"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        String requestJson = """
            {
                "email": "juan@example.com",
                "password": "Password123@"
            }
            """;

        when(userService.login(any(LoginRequest.class))).thenReturn(
            LoginResponse.builder()
                .token("jwt.token.here")
                .userId(1L)
                .email("juan@example.com")
                .expiresIn(3600000L)
                .build()
        );

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt.token.here"))
            .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        String requestJson = """
            {
                "email": "juan@example.com",
                "password": "WrongPassword"
            }
            """;

        when(userService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/users/logout")
                .header("Authorization", "Bearer valid.token")
                .header("X-User-Id", "1"))
            .andExpect(status().isOk());
    }

    @Test
    void testValidateToken_Valid() throws Exception {
        when(userService.isTokenValid(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/users/token/validate")
                .param("token", "valid.token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testValidateToken_Invalid() throws Exception {
        when(userService.isTokenValid(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/users/token/validate")
                .param("token", "invalid.token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void testUpdateUser_EmptyRequest_ShouldReturnBadRequest() throws Exception {
        String emptyRequestJson = "{}";

        mockMvc.perform(patch("/api/users/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUser_OnlyEmail_ShouldSucceed() throws Exception {
        String requestJson = """
            {
                "email": "newemail@example.com"
            }
            """;

        when(userService.updateUser(any(Long.class), any())).thenReturn(
            UserResponse.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Perez")
                .dni("12345678")
                .email("newemail@example.com")
                .phone("5491155555555")
                .build()
        );

        mockMvc.perform(patch("/api/users/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("newemail@example.com"));
    }
}
