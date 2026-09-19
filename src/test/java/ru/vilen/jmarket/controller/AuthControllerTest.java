package ru.vilen.jmarket.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.dto.LoginRequest;
import ru.vilen.jmarket.dto.TokenResponse;
import ru.vilen.jmarket.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_shouldReturnTokenResponse_whenRequestIsValid() throws Exception {
        TokenResponse response = new TokenResponse(
                "encoded-access-token",
                "Bearer",
                900
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("encoded-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest actualRequest = captor.getValue();
        assertThat(actualRequest.email()).isEqualTo("ivan@example.com");
        assertThat(actualRequest.password()).isEqualTo("password");
    }

    @Test
    void login_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturnBadRequest_whenEmailIsTooLong() throws Exception {
        String tooLongEmail = "a".repeat(244) + "@example.com";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password"
                                }
                                """.formatted(tooLongEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@example.com",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturnBadRequest_whenPasswordIsTooLong() throws Exception {
        String tooLongPassword = "a".repeat(73);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@example.com",
                                  "password": "%s"
                                }
                                """.formatted(tooLongPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(authService, never()).login(any(LoginRequest.class));
    }
}
