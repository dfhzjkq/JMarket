package ru.vilen.jmarket.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.dto.UserCreateRequest;
import ru.vilen.jmarket.dto.UserResponse;
import ru.vilen.jmarket.exception.EmailAlreadyExistsException;
import ru.vilen.jmarket.service.UserService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void create_shouldReturnUserResponse_whenRequestIsValid() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "ivan",
                "ivan@example.com"
        );

        when(userService.create(any(UserCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "name": "ivan",
                    "email": "ivan@example.com",
                    "password": "password"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("ivan"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"));

        ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).create(captor.capture());
        UserCreateRequest actualRequest = captor.getValue();

        assertThat(actualRequest.name()).isEqualTo("ivan");
        assertThat(actualRequest.email()).isEqualTo("ivan@example.com");
        assertThat(actualRequest.password()).isEqualTo("password");
    }

    @Test
    void create_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "name": "",
                    "email": "ivan@example.com",
                    "password": "password"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "123",
                    "password": "password"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenEmailIsTooLong() throws Exception {
        String tooLongEmail = "a".repeat(244) + "@example.com";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "%s",
                    "password": "password"
                }
                """.formatted(tooLongEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        when(userService.create(any(UserCreateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ivan@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "ivan@example.com",
                    "password": "password"
                }
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("User with email ivan@example.com already exists"));

        verify(userService).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "ivan@example.com",
                    "password": ""
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "ivan@example.com",
                    "password": "123"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenPasswordIsTooLong() throws Exception {
        String tooLongPassword = "a".repeat(73);
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "name": "ivan",
                    "email": "ivan@example.com",
                    "password": "%s"
                }
                """.formatted(tooLongPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(userService, never()).create(any(UserCreateRequest.class));
    }
}
