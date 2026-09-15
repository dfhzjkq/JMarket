package ru.vilen.jmarket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.controller.AdController;
import ru.vilen.jmarket.controller.UserController;
import ru.vilen.jmarket.dto.AdCreateRequest;
import ru.vilen.jmarket.service.AdService;
import ru.vilen.jmarket.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdController.class,
        UserController.class,
})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdService adService;

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
                          "price": 15000.00
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
    @WithMockUser(username = "ivan@example.com")
    void createAd_shouldReturnCreated_whenUserIsAuthenticated() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                        {
                          "title": "Phone",
                          "description": "Good phone",
                          "price": 15000.00
                        }
                        """))
                .andExpect(status().isCreated());

        verify(adService).create(any(AdCreateRequest.class), eq("ivan@example.com"));
    }
}
