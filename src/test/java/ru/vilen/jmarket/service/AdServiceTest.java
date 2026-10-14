package ru.vilen.jmarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.vilen.jmarket.dto.*;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.AdStatus;
import ru.vilen.jmarket.entity.Category;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.exception.AdAccessDeniedException;
import ru.vilen.jmarket.exception.AdNotFoundException;
import ru.vilen.jmarket.exception.CategoryNotFoundException;
import ru.vilen.jmarket.exception.UserNotFoundException;
import ru.vilen.jmarket.repository.AdRepository;
import ru.vilen.jmarket.repository.CategoryRepository;
import ru.vilen.jmarket.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdServiceTest {

    @InjectMocks
    private AdService adService;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void create_shouldReturnAdResponse_whenAuthorExists() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        Category category = new Category();
        category.setId(1L);
        category.setName("Электроника");

        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                1L
        );

        Ad savedAd = new Ad();
        savedAd.setId(10L);
        savedAd.setTitle("Phone");
        savedAd.setDescription("Good phone");
        savedAd.setPrice(new BigDecimal("15000.00"));
        savedAd.setAuthor(author);
        savedAd.setStatus(AdStatus.ACTIVE);
        savedAd.setCategory(category);

        when(userRepository.findByEmail(authorEmail))
                .thenReturn(Optional.of(author));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(adRepository.save(any(Ad.class)))
                .thenReturn(savedAd);

        AdResponse actual = adService.create(request, authorEmail);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("Phone");
        assertThat(actual.description()).isEqualTo("Good phone");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);
        assertThat(actual.status()).isEqualTo(AdStatus.ACTIVE);
        assertThat(actual.categoryId()).isEqualTo(1L);
        verify(userRepository).findByEmail(authorEmail);
        verify(categoryRepository).findById(1L);

        ArgumentCaptor<Ad> adCaptor = ArgumentCaptor.forClass(Ad.class);
        verify(adRepository).save(adCaptor.capture());

        Ad actualAdToSave = adCaptor.getValue();
        assertThat(actualAdToSave.getTitle()).isEqualTo(request.title());
        assertThat(actualAdToSave.getDescription()).isEqualTo(request.description());
        assertThat(actualAdToSave.getPrice())
                .isEqualByComparingTo(request.price());
        assertThat(actualAdToSave.getAuthor()).isSameAs(author);
        assertThat(actualAdToSave.getStatus()).isEqualTo(AdStatus.ACTIVE);
        assertThat(actualAdToSave.getCategory()).isSameAs(category);
    }

    @Test
    void create_shouldThrowUserNotFoundException_whenAuthorDoesNotExist() {
        String authorEmail = "missing@example.com";

        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                1L
        );

        when(userRepository.findByEmail(authorEmail))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.create(request, authorEmail))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with email missing@example.com not found");

        verify(userRepository).findByEmail(authorEmail);
        verify(adRepository, never()).save(any(Ad.class));
    }

    @Test
    void create_shouldThrowCategoryNotFoundException_whenCategoryDoesNotExist() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                999L
        );

        when(userRepository.findByEmail(authorEmail))
                .thenReturn(Optional.of(author));
        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.create(request, authorEmail))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with ID 999 not found");

        verify(categoryRepository).findById(999L);
        verify(adRepository, never()).save(any(Ad.class));
    }

    @Test
    void findById_shouldReturnAdResponse_whenAdExists() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);
        ad.setCategory(new Category(1L, "Электроника"));

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdResponse actual = adService.findById(10L);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("Phone");
        assertThat(actual.description()).isEqualTo("Good phone");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);
        assertThat(actual.status()).isEqualTo(AdStatus.ACTIVE);
        assertThat(actual.categoryId()).isEqualTo(1L);

        verify(adRepository).findById(10L);
    }

    @Test
    void findById_shouldThrowAdNotFoundException_whenAdDoesNotExist() {
        when(adRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.findById(999L))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
    }

    @Test
    void findAll_shouldReturnAdPageResponse_whenAdsExist() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        Category category = new Category();
        category.setId(1L);
        category.setName("Электроника");

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);
        ad.setCategory(category);

        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> adPage = new PageImpl<>(
                List.of(ad),
                pageable,
                1
        );


        AdFilter adFilter = new AdFilter(
                null,
                null,
                null,
                null,
                null
        );

        when(adRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(adPage);

        AdPageResponse actual = adService.findAll(pageable, adFilter);

        AdResponse response = new AdResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getDescription(),
                ad.getPrice(),
                ad.getAuthor().getId(),
                ad.getStatus(),
                ad.getCategory().getId()
        );

        assertThat(actual.content()).isEqualTo(List.of(response));
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(1);
        assertThat(actual.totalPages()).isEqualTo(1);
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(adRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    void findAll_shouldReturnEmptyPage_whenNoAdsExist() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Ad> emptyPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        AdFilter adFilter = new AdFilter(
                null,
                null,
                null,
                null,
                null
        );

        when(adRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(emptyPage);

        AdPageResponse actual = adService.findAll(pageable, adFilter);
        assertThat(actual.content()).isEmpty();
        assertThat(actual.page()).isZero();
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isZero();
        assertThat(actual.totalPages()).isZero();
        assertThat(actual.first()).isTrue();
        assertThat(actual.last()).isTrue();

        verify(adRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    void update_shouldReturnUpdatedAdResponse_whenAdExists() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Old Phone");
        ad.setDescription("Old phone description");
        ad.setPrice(new BigDecimal("10000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);
        ad.setCategory(new Category(1L, "Электроника"));

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdResponse actual = adService.update(10L, new AdUpdateRequest(
                "New Phone",
                "New phone description",
                new BigDecimal("15000.00")
        ), authorEmail);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("New Phone");
        assertThat(actual.description()).isEqualTo("New phone description");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);
        assertThat(actual.status()).isEqualTo(AdStatus.ACTIVE);
        assertThat(actual.categoryId()).isEqualTo(1L);

        assertThat(ad.getTitle()).isEqualTo("New Phone");
        assertThat(ad.getDescription()).isEqualTo("New phone description");
        assertThat(ad.getPrice()).isEqualByComparingTo("15000.00");

        verify(adRepository).findById(10L);
    }

    @Test
    void update_shouldThrowAdNotFoundException_whenAdDoesNotExist() {
        when(adRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.update(999L, new AdUpdateRequest(
                "New Phone",
                "New phone description",
                new BigDecimal("15000.00")
        ), "ivan@example.com"))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
    }

    @Test
    void update_shouldThrowAdAccessDeniedException_whenUserIsNotAuthor() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Old Phone");
        ad.setDescription("Old phone description");
        ad.setPrice(new BigDecimal("10000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);

        AdUpdateRequest request = new AdUpdateRequest(
                "New Phone",
                "New phone description",
                new BigDecimal("15000.00")
        );

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.update(10L, request, "petr@example.com"))
                .isInstanceOf(AdAccessDeniedException.class)
                .hasMessage("You do not have permission to manage ad with ID 10");

        assertThat(ad.getTitle()).isEqualTo("Old Phone");
        assertThat(ad.getDescription()).isEqualTo("Old phone description");
        assertThat(ad.getPrice()).isEqualByComparingTo("10000.00");

        verify(adRepository).findById(10L);
    }

    @Test
    void updateStatus_shouldReturnUpdatedAdResponse_whenUserIsAuthor() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);
        ad.setCategory(new Category(1L, "Электроника"));

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdStatusUpdateRequest request =
                new AdStatusUpdateRequest(AdStatus.SOLD);

        AdResponse actual = adService.updateStatus(10L, request, authorEmail);

        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.status()).isEqualTo(AdStatus.SOLD);
        assertThat(actual.categoryId()).isEqualTo(1L);
        assertThat(ad.getStatus()).isEqualTo(AdStatus.SOLD);

        verify(adRepository).findById(10L);
    }

    @Test
    void updateStatus_shouldThrowAdNotFoundException_whenAdDoesNotExist() {
        when(adRepository.findById(999L))
                .thenReturn(Optional.empty());

        AdStatusUpdateRequest request =
                new AdStatusUpdateRequest(AdStatus.SOLD);

        assertThatThrownBy(() ->
                adService.updateStatus(999L, request, "ivan@example.com"))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
    }

    @Test
    void updateStatus_shouldThrowAdAccessDeniedException_whenUserIsNotAuthor() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdStatusUpdateRequest request =
                new AdStatusUpdateRequest(AdStatus.SOLD);

        assertThatThrownBy(() ->
                adService.updateStatus(10L, request, "petr@example.com"))
                .isInstanceOf(AdAccessDeniedException.class)
                .hasMessage("You do not have permission to manage ad with ID 10");

        assertThat(ad.getStatus()).isEqualTo(AdStatus.ACTIVE);
        verify(adRepository).findById(10L);
    }

    @Test
    void delete_shouldDeleteAd_whenAdExists() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        adService.delete(10L, authorEmail);

        verify(adRepository).findById(10L);
        verify(adRepository).delete(ad);
    }

    @Test
    void delete_shouldThrowAdNotFoundException_whenAdDoesNotExist() {
        when(adRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.delete(999L, "ivan@example.com"))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
        verify(adRepository, never()).delete(any(Ad.class));
    }

    @Test
    void delete_shouldThrowAdAccessDeniedException_whenUserIsNotAuthor() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);
        ad.setStatus(AdStatus.ACTIVE);

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.delete(10L, "petr@example.com"))
                .isInstanceOf(AdAccessDeniedException.class)
                .hasMessage("You do not have permission to manage ad with ID 10");

        verify(adRepository).findById(10L);
        verify(adRepository, never()).delete(any(Ad.class));
    }
}
