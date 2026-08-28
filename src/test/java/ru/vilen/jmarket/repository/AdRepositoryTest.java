package ru.vilen.jmarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import ru.vilen.jmarket.TestcontainersConfiguration;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.User;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class AdRepositoryTest {

    @Autowired
    private AdRepository adRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindById_shouldReturnSavedAd() {
        User author = new User();
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        User savedAuthor = userRepository.save(author);

        Ad ad = new Ad();
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(savedAuthor);

        Ad savedAd = adRepository.save(ad);

        entityManager.flush();
        entityManager.clear();

        Ad foundAd = adRepository.findById(savedAd.getId())
                .orElseThrow();

        assertThat(foundAd.getId()).isEqualTo(savedAd.getId());
        assertThat(foundAd.getTitle()).isEqualTo("Phone");
        assertThat(foundAd.getDescription()).isEqualTo("Good phone");
        assertThat(foundAd.getPrice()).isEqualByComparingTo("15000.00");

        assertThat(foundAd.getAuthor().getId()).isEqualTo(savedAuthor.getId());
        assertThat(foundAd.getAuthor().getEmail()).isEqualTo("ivan@example.com");
    }
}
