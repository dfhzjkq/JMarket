package ru.vilen.jmarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vilen.jmarket.dto.AdCreateRequest;
import ru.vilen.jmarket.dto.AdResponse;
import ru.vilen.jmarket.dto.AdUpdateRequest;
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
public class AdServiceTest {

    @InjectMocks
    private AdService adService;

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void create_shouldReturnAdResponse_whenAuthorExists() {
        User author = new User();
        author.setId(1L);
        author.setName("Ivan");
        author.setEmail("ivan@example.com");

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

        when(userRepository.findById(request.authorId()))
                .thenReturn(Optional.of(author));
        when(adRepository.save(any(Ad.class)))
                .thenReturn(savedAd);

        AdResponse actual = adService.create(request);
        assertThat(actual.id()).isEqualTo(10L);
        assertThat(actual.title()).isEqualTo("Phone");
        assertThat(actual.description()).isEqualTo("Good phone");
        assertThat(actual.price()).isEqualByComparingTo("15000.00");
        assertThat(actual.authorId()).isEqualTo(1L);
        verify(userRepository).findById(request.authorId());

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
        AdCreateRequest request = new AdCreateRequest(
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                999L
        );

        when(userRepository.findById(request.authorId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.create(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with ID 999 not found");

        verify(userRepository).findById(request.authorId());
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
    void findAll_shouldReturnListOfAdResponses_whenAdsExist() {
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

        when(adRepository.findAll())
                .thenReturn(List.of(ad));

        List<AdResponse> actual = adService.findAll();

        assertThat(actual).hasSize(1);

        AdResponse response = actual.getFirst();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Phone");
        assertThat(response.description()).isEqualTo("Good phone");
        assertThat(response.price()).isEqualByComparingTo("15000.00");
        assertThat(response.authorId()).isEqualTo(1L);
        verify(adRepository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoAdsExist() {
        when(adRepository.findAll())
                .thenReturn(List.of());

        List<AdResponse> actual = adService.findAll();
        assertThat(actual).isEmpty();
        verify(adRepository).findAll();
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
}
