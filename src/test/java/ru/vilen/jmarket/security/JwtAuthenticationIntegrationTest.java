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
import ru.vilen.jmarket.repository.CategoryRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void registeredUser_shouldCreateAndUpdateAdStatus_withIssuedAccessToken() throws Exception {
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

        Long categoryId = categoryRepository.findByName("Электроника")
                .orElseThrow()
                .getId();

        String createAdResponse = mockMvc.perform(post("/api/ads")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00,
                                  "categoryId": %d
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Phone"))
                .andExpect(jsonPath("$.authorId").value(authorId.longValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number adId = JsonPath.read(createAdResponse, "$.id");

        mockMvc.perform(patch("/api/ads/{id}/status", adId.longValue())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adId.longValue()))
                .andExpect(jsonPath("$.status").value("SOLD"));

        mockMvc.perform(get("/api/ads/{id}", adId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
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
