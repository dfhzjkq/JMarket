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
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.exception.AdNotFoundException;
import ru.vilen.jmarket.exception.UserNotFoundException;
import ru.vilen.jmarket.repository.AdRepository;
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

    @Test
    void create_shouldReturnAdResponse_whenAuthorExists() {
        String authorEmail = "ivan@example.com";

        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail(authorEmail);

        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00")
        );

        Ad savedAd = new Ad();
        savedAd.setId(10L);
        savedAd.setTitle("Phone");
        savedAd.setDescription("Good phone");
        savedAd.setPrice(new BigDecimal("15000.00"));
        savedAd.setAuthor(author);

        when(userRepository.findByEmail(authorEmail))
                .thenReturn(Optional.of(author));
        when(adRepository.save(any(Ad.class)))
                .thenReturn(savedAd);

        AdResponse actual = adService.create(request, authorEmail);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("Phone");
        assertThat(actual.description()).isEqualTo("Good phone");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);
        verify(userRepository).findByEmail(authorEmail);

        ArgumentCaptor<Ad> adCaptor = ArgumentCaptor.forClass(Ad.class);
        verify(adRepository).save(adCaptor.capture());

        Ad actualAdToSave = adCaptor.getValue();
        assertThat(actualAdToSave.getTitle()).isEqualTo(request.title());
        assertThat(actualAdToSave.getDescription()).isEqualTo(request.description());
        assertThat(actualAdToSave.getPrice())
                .isEqualByComparingTo(request.price());
        assertThat(actualAdToSave.getAuthor()).isSameAs(author);
    }

    @Test
    void create_shouldThrowUserNotFoundException_whenAuthorDoesNotExist() {
        String authorEmail = "missing@example.com";

        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00")
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

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdResponse actual = adService.findById(10L);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("Phone");
        assertThat(actual.description()).isEqualTo("Good phone");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);

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

        Ad ad = new Ad();
        ad.setId(10L);
        ad.setTitle("Phone");
        ad.setDescription("Good phone");
        ad.setPrice(new BigDecimal("15000.00"));
        ad.setAuthor(author);

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
                ad.getAuthor().getId()
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

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        AdResponse actual = adService.update(10L, new AdUpdateRequest(
                "New Phone",
                "New phone description",
                new BigDecimal("15000.00")
        ));
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("New Phone");
        assertThat(actual.description()).isEqualTo("New phone description");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);

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
        )))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
    }

    @Test
    void delete_shouldDeleteAd_whenAdExists() {
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

        when(adRepository.findById(10L))
                .thenReturn(Optional.of(ad));

        adService.delete(10L);

        verify(adRepository).findById(10L);
        verify(adRepository).delete(ad);
    }

    @Test
    void delete_shouldThrowAdNotFoundException_whenAdDoesNotExist() {
        when(adRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.delete(999L))
                .isInstanceOf(AdNotFoundException.class)
                .hasMessage("Ad with ID 999 not found");

        verify(adRepository).findById(999L);
        verify(adRepository, never()).delete(any(Ad.class));
    }
}
