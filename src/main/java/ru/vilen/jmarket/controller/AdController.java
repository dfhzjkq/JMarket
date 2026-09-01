package ru.vilen.jmarket.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vilen.jmarket.dto.*;
import ru.vilen.jmarket.service.AdService;

@RestController
@RequestMapping("/api/ads")
@AllArgsConstructor
public class AdController {

    private final AdService adService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdResponse create(@Valid @RequestBody AdCreateRequest request) {
        return adService.create(request);
    }

    @GetMapping
    public AdPageResponse findAll(
            @PageableDefault(
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            @Valid @ModelAttribute AdFilter adFilter
    ) {
        return adService.findAll(pageable, adFilter);
    }

    @GetMapping("/{id}")
    public AdResponse findById(@PathVariable Long id) {
        return adService.findById(id);
    }

    @PutMapping("/{id}")
    public AdResponse update(@PathVariable Long id, @Valid @RequestBody AdUpdateRequest request) {
        return adService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adService.delete(id);
    }
}
