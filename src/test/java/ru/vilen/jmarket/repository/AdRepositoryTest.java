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
import ru.vilen.jmarket.entity.AdStatus;
import ru.vilen.jmarket.entity.Category;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindById_shouldReturnSavedAd() {
        User savedAuthor = saveUser("Ivan", "ivan@example.com");
        Ad savedAd = saveAd(
                "Phone",
                "Good phone",
                "15000.00",
                savedAuthor,
                AdStatus.ACTIVE,
                "Электроника"
        );

        entityManager.flush();
        entityManager.clear();

        Ad foundAd = adRepository.findById(savedAd.getId())
                .orElseThrow();

        assertThat(foundAd.getId()).isEqualTo(savedAd.getId());
        assertThat(foundAd.getTitle()).isEqualTo("Phone");
        assertThat(foundAd.getDescription()).isEqualTo("Good phone");
        assertThat(foundAd.getPrice()).isEqualByComparingTo("15000.00");
        assertThat(foundAd.getStatus()).isEqualTo(AdStatus.ACTIVE);
        assertThat(foundAd.getCategory().getId())
                .isEqualTo(savedAd.getCategory().getId());
        assertThat(foundAd.getCategory().getName()).isEqualTo("Электроника");

        assertThat(foundAd.getAuthor().getId()).isEqualTo(savedAuthor.getId());
        assertThat(foundAd.getAuthor().getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    void findAll_shouldReturnAdsWithinPriceRange_whenPriceSpecificationsAreApplied() {
        User author = saveUser("Ivan", "ivan@example.com");
        saveAd(
                "PS5",
                "New PS5",
                "50000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "PS5 Slim",
                "New PS5 Slim",
                "55000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "PS5 Pro",
                "New PS5 Pro",
                "62000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );

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
        User savedAuthor1 = saveUser("Ivan", "ivan@example.com");
        User savedAuthor2 = saveUser("Sergey", "sergey@example.com");
        saveAd(
                "PS5",
                "New PS5",
                "50000.00",
                savedAuthor1,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "PS5 Slim",
                "New PS5 Slim",
                "55000.00",
                savedAuthor2,
                AdStatus.ACTIVE,
                "Электроника"
        );

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
        User author = saveUser("Ivan", "ivan@example.com");
        saveAd(
                "Phone",
                "New",
                "50000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "Accessories",
                "Case for PHONE",
                "3000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "Laptop",
                "Gaming computer",
                "120000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );

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

    @Test
    void findAll_shouldReturnOnlySoldAds_whenStatusSpecificationIsApplied() {
        User author = saveUser("Ivan", "ivan@example.com");
        saveAd(
                "Phone",
                "New",
                "50000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "Accessories",
                "Case for PHONE",
                "3000.00",
                author,
                AdStatus.SOLD,
                "Электроника"
        );
        saveAd(
                "Laptop",
                "Gaming computer",
                "120000.00",
                author,
                AdStatus.ARCHIVED,
                "Электроника"
        );

        entityManager.flush();
        entityManager.clear();

        Specification<Ad> specification =
                AdSpecifications.hasStatus(AdStatus.SOLD);

        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = adRepository.findAll(specification, pageable);

        assertThat(adPage.getContent())
                .singleElement()
                .satisfies(ad -> assertThat(ad.getStatus()).isEqualTo(AdStatus.SOLD));
    }

    @Test
    void findAll_shouldReturnOnlyCategoryAds_whenCategorySpecificationIsApplied() {
        User author = saveUser("Ivan", "ivan@example.com");
        saveAd(
                "Phone",
                "New phone",
                "50000.00",
                author,
                AdStatus.ACTIVE,
                "Электроника"
        );
        saveAd(
                "Bicycle",
                "Mountain bike",
                "30000.00",
                author,
                AdStatus.ACTIVE,
                "Транспорт"
        );

        entityManager.flush();
        entityManager.clear();

        Long electronicsId = findCategory("Электроника").getId();

        Specification<Ad> specification =
                AdSpecifications.hasCategoryId(electronicsId);

        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = adRepository.findAll(specification, pageable);

        assertThat(adPage.getContent())
                .singleElement()
                .satisfies(ad -> {
                    assertThat(ad.getTitle()).isEqualTo("Phone");
                    assertThat(ad.getCategory().getId()).isEqualTo(electronicsId);
                });
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
            AdStatus status,
            String categoryName
    ) {
        Ad ad = new Ad();
        ad.setTitle(title);
        ad.setDescription(description);
        ad.setPrice(new BigDecimal(price));
        ad.setAuthor(author);
        ad.setStatus(status);
        ad.setCategory(findCategory(categoryName));

        return adRepository.save(ad);
    }

    private Category findCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow();
    }
}
