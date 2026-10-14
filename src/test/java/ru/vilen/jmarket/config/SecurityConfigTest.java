package ru.vilen.jmarket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.controller.AdController;
import ru.vilen.jmarket.controller.AuthController;
import ru.vilen.jmarket.controller.UserController;
import ru.vilen.jmarket.dto.AdCreateRequest;
import ru.vilen.jmarket.dto.AdStatusUpdateRequest;
import ru.vilen.jmarket.dto.LoginRequest;
import ru.vilen.jmarket.dto.TokenResponse;
import ru.vilen.jmarket.service.AdService;
import ru.vilen.jmarket.service.AuthService;
import ru.vilen.jmarket.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdController.class,
        AuthController.class,
        UserController.class,
})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdService adService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserService userService;

    @Test
    void createAd_shouldReturnUnauthorized_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/api/ads")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                        {
                          "title": "Phone",
                          "description": "Good phone",
                          "price": 15000.00,
                          "categoryId": 1
                        }
                        """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adService);
    }

    @Test
    void findAllAds_shouldReturnOk_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(get("/api/ads")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_shouldReturnCreated_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "ivan",
                            "email": "ivan@example.com",
                            "password": "password"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void login_shouldReturnOk_whenUserIsAnonymous() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new TokenResponse("encoded-access-token", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("encoded-access-token"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void createAd_shouldReturnCreated_whenUserIsAuthenticated() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .with(jwt().jwt(token -> token.subject("ivan@example.com")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                        {
                          "title": "Phone",
                          "description": "Good phone",
                          "price": 15000.00,
                          "categoryId": 1
                        }
                        """))
                .andExpect(status().isCreated());

        verify(adService).create(any(AdCreateRequest.class), eq("ivan@example.com"));
    }

    @Test
    void deleteAd_shouldReturnUnauthorized_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/ads/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adService);
    }

    @Test
    void deleteAd_shouldReturnNoContent_whenUserIsAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/ads/1")
                        .with(jwt().jwt(token -> token.subject("ivan@example.com"))))
                .andExpect(status().isNoContent());

        verify(adService).delete(1L, "ivan@example.com");
    }

    @Test
    void updateAdStatus_shouldReturnUnauthorized_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(patch("/api/ads/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adService);
    }

    @Test
    void updateAdStatus_shouldReturnOk_whenUserIsAuthenticated() throws Exception {
        mockMvc.perform(patch("/api/ads/1/status")
                        .with(jwt().jwt(token -> token.subject("ivan@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isOk());

        verify(adService).updateStatus(
                eq(1L),
                any(AdStatusUpdateRequest.class),
                eq("ivan@example.com")
        );
    }
}
