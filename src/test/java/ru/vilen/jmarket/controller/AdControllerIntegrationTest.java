package ru.vilen.jmarket.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.TestcontainersConfiguration;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.AdStatus;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.repository.AdRepository;
import ru.vilen.jmarket.repository.CategoryRepository;
import ru.vilen.jmarket.repository.UserRepository;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "jmarket.security.jwt.secret=" +
                "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdRepository adRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        adRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findAll_shouldReturnOnlyActiveAds_whenStatusIsNotSpecified() throws Exception {
        User user = saveUser("ivan", "ivan@example.com");

        saveAd("ad1", "ad1 desc", "10000.00", user, AdStatus.ACTIVE);
        saveAd("ad2", "ad2 desc", "10000.00", user, AdStatus.SOLD);
        saveAd("ad3", "ad3 desc", "10000.00", user, AdStatus.ARCHIVED);

        mockMvc.perform(get("/api/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void findAll_shouldReturnOnlySoldAds_whenSoldStatusIsSpecified() throws Exception {
        User user = saveUser("ivan", "ivan@example.com");

        saveAd("ad1", "ad1 desc", "10000.00", user, AdStatus.ACTIVE);
        saveAd("ad2", "ad2 desc", "10000.00", user, AdStatus.SOLD);
        saveAd("ad3", "ad3 desc", "10000.00", user, AdStatus.ARCHIVED);

        mockMvc.perform(get("/api/ads").param("status", "SOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SOLD"));
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("encoded-password");

        return userRepository.save(user);
    }

    private Ad saveAd(
            String title,
            String description,
            String price,
            User author,
            AdStatus status
    ) {
        Ad ad = new Ad();
        ad.setTitle(title);
        ad.setDescription(description);
        ad.setPrice(new BigDecimal(price));
        ad.setAuthor(author);
        ad.setStatus(status);
        ad.setCategory(categoryRepository.findByName("Электроника").orElseThrow());

        return adRepository.save(ad);
    }
}
