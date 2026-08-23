package ru.vilen.jmarket.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vilen.jmarket.dto.AdCreateRequest;
import ru.vilen.jmarket.dto.AdResponse;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.exception.UserNotFoundException;
import ru.vilen.jmarket.repository.AdRepository;
import ru.vilen.jmarket.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class AdService {

    private final AdRepository adRepository;
    private final UserRepository userRepository;

    @Transactional
    public AdResponse create(AdCreateRequest request) {
        User author = userRepository.findById(request.authorId())
                .orElseThrow(() -> new UserNotFoundException(request.authorId()));

        Ad ad = toEntity(request, author);
        Ad savedAd = adRepository.save(ad);
        return toResponse(savedAd);
    }

    @Transactional(readOnly = true)
    public List<AdResponse> findAll() {
        return adRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Ad toEntity(AdCreateRequest request, User author) {
        Ad ad = new Ad();
        ad.setTitle(request.title());
        ad.setDescription(request.description());
        ad.setPrice(request.price());
        ad.setAuthor(author);
        return ad;
    }

    private AdResponse toResponse(Ad ad) {
        return new AdResponse(ad.getId(), ad.getTitle(), ad.getDescription(), ad.getPrice(), ad.getAuthor().getId());
    }
}
