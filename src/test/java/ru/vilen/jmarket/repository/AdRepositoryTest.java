package ru.vilen.jmarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.vilen.jmarket.TestcontainersConfiguration;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.specification.AdSpecifications;

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

    @Test
    void findAll_shouldReturnAdsWithinPriceRange_whenPriceSpecificationsAreApplied() {
        User author = new User();
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        User savedAuthor = userRepository.save(author);

        Ad ad1 = new Ad();
        ad1.setTitle("PS5");
        ad1.setDescription("New PS5");
        ad1.setPrice(new BigDecimal("50000.00"));
        ad1.setAuthor(savedAuthor);
        adRepository.save(ad1);

        Ad ad2 = new Ad();
        ad2.setTitle("PS5 Slim");
        ad2.setDescription("New PS5 Slim");
        ad2.setPrice(new BigDecimal("55000.00"));
        ad2.setAuthor(savedAuthor);
        adRepository.save(ad2);

        Ad ad3 = new Ad();
        ad3.setTitle("PS5 Pro");
        ad3.setDescription("New PS5 Pro");
        ad3.setPrice(new BigDecimal("62000.00"));
        ad3.setAuthor(savedAuthor);
        adRepository.save(ad3);

        entityManager.flush();
        entityManager.clear();

        Specification<Ad> specification = Specification.allOf(
                AdSpecifications.hasMinPrice(new BigDecimal("51000.00")),
                AdSpecifications.hasMaxPrice(new BigDecimal("60000.00"))
        );
        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = adRepository.findAll(specification, pageable);
        assertThat(adPage.getTotalElements()).isEqualTo(1);

        assertThat(adPage.getContent())
                .singleElement()
                .satisfies(ad -> {
                    assertThat(ad.getTitle()).isEqualTo("PS5 Slim");
                    assertThat(ad.getPrice()).isEqualByComparingTo("55000.00");
                });
    }

    @Test
    void findAll_shouldReturnOnlyAuthorsAds_whenAuthorIdSpecificationIsApplied() {
        User author1 = new User();
        author1.setName("Ivan");
        author1.setEmail("ivan@example.com");
        User savedAuthor1 = userRepository.save(author1);

        User author2 = new User();
        author2.setName("Sergey");
        author2.setEmail("sergey@example.com");
        User savedAuthor2 = userRepository.save(author2);

        Ad ad1 = new Ad();
        ad1.setTitle("PS5");
        ad1.setDescription("New PS5");
        ad1.setPrice(new BigDecimal("50000.00"));
        ad1.setAuthor(savedAuthor1);
        adRepository.save(ad1);

        Ad ad2 = new Ad();
        ad2.setTitle("PS5 Slim");
        ad2.setDescription("New PS5 Slim");
        ad2.setPrice(new BigDecimal("55000.00"));
        ad2.setAuthor(savedAuthor2);
        adRepository.save(ad2);

        entityManager.flush();
        entityManager.clear();

        Specification<Ad> specification = AdSpecifications.hasAuthorId(savedAuthor1.getId());
        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = adRepository.findAll(specification, pageable);

        assertThat(adPage.getContent())
                .singleElement()
                .satisfies(ad -> {
                    assertThat(ad.getTitle()).isEqualTo("PS5");
                    assertThat(ad.getPrice()).isEqualByComparingTo("50000.00");
                    assertThat(ad.getAuthor().getId())
                            .isEqualTo(savedAuthor1.getId());
                });
    }

    @Test
    void findAll_shouldReturnAdsMatchingTitleOrDescription_whenQuerySpecificationIsApplied() {
        User author = new User();
        author.setName("Ivan");
        author.setEmail("ivan@example.com");
        userRepository.save(author);

        Ad ad1 = new Ad();
        ad1.setTitle("Phone");
        ad1.setDescription("New");
        ad1.setPrice(new BigDecimal("50000.00"));
        ad1.setAuthor(author);
        adRepository.save(ad1);

        Ad ad2 = new Ad();
        ad2.setTitle("Accessories");
        ad2.setDescription("Case for PHONE");
        ad2.setPrice(new BigDecimal("3000.00"));
        ad2.setAuthor(author);
        adRepository.save(ad2);

        Ad ad3 = new Ad();
        ad3.setTitle("Laptop");
        ad3.setDescription("Gaming computer");
        ad3.setPrice(new BigDecimal("120000.00"));
        ad3.setAuthor(author);
        adRepository.save(ad3);

        entityManager.flush();
        entityManager.clear();

        Specification<Ad> specification =
                AdSpecifications.hasQuery("phone");

        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = adRepository.findAll(specification, pageable);

        assertThat(adPage.getTotalElements()).isEqualTo(2);
        assertThat(adPage.getContent())
                .extracting(Ad::getTitle)
                .containsExactlyInAnyOrder("Phone", "Accessories");
    }
}
