package ru.vilen.jmarket.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.vilen.jmarket.dto.AdCreateRequest;
import ru.vilen.jmarket.dto.AdResponse;
import ru.vilen.jmarket.dto.AdUpdateRequest;
import ru.vilen.jmarket.service.AdService;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@AllArgsConstructor
public class AdController {

    private final AdService adService;

    @PostMapping
    public AdResponse create(@Valid @RequestBody AdCreateRequest request) {
        return adService.create(request);
    }

    @GetMapping
    public List<AdResponse> findAll() {
        return adService.findAll();
    }

    @GetMapping("/{id}")
    public AdResponse findById(@PathVariable Long id) {
        return adService.findById(id);
    }

    @PutMapping("/{id}")
    public AdResponse update(@PathVariable Long id, @Valid @RequestBody AdUpdateRequest request) {
        return adService.update(id, request);
    }
}
