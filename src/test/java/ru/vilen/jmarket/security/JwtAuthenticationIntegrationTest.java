package ru.vilen.jmarket.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "jmarket.security.jwt.secret=" +
                "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeredUser_shouldCreateAd_withIssuedAccessToken() throws Exception {
        String registrationResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ivan",
                                  "email": "ivan.jwt@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number authorId = JsonPath.read(registrationResponse, "$.id");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan.jwt@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Phone"))
                .andExpect(jsonPath("$.authorId").value(authorId.longValue()));
    }

    @Test
    void createAd_shouldReturnUnauthorized_withoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAd_shouldReturnUnauthorized_whenAccessTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
