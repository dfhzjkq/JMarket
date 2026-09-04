package ru.vilen.jmarket.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vilen.jmarket.dto.*;
import ru.vilen.jmarket.entity.Ad;
import ru.vilen.jmarket.entity.User;
import ru.vilen.jmarket.exception.AdNotFoundException;
import ru.vilen.jmarket.exception.UserNotFoundException;
import ru.vilen.jmarket.repository.AdRepository;
import ru.vilen.jmarket.repository.UserRepository;

import org.springframework.data.domain.Pageable;
import ru.vilen.jmarket.specification.AdSpecifications;

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
    public AdPageResponse findAll(Pageable pageable, AdFilter adFilter) {
        Specification<Ad> specification = Specification.allOf(
                AdSpecifications.hasQuery(adFilter.query()),
                AdSpecifications.hasMinPrice(adFilter.minPrice()),
                AdSpecifications.hasMaxPrice(adFilter.maxPrice()),
                AdSpecifications.hasAuthorId(adFilter.authorId())
        );

        Page<Ad> adPage = adRepository.findAll(specification, pageable);

        List<AdResponse> content = adPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new AdPageResponse(
                content,
                adPage.getNumber(),
                adPage.getSize(),
                adPage.getTotalElements(),
                adPage.getTotalPages(),
                adPage.isFirst(),
                adPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public AdResponse findById(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new AdNotFoundException(id));
        return toResponse(ad);
    }

    @Transactional
    public AdResponse update(Long id, AdUpdateRequest request) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new AdNotFoundException(id));

        ad.setTitle(request.title());
        ad.setDescription(request.description());
        ad.setPrice(request.price());

        return toResponse(ad);
    }

    @Transactional
    public void delete(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new AdNotFoundException(id));
        adRepository.delete(ad);
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
