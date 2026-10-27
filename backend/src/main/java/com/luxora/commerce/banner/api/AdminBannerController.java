package com.luxora.commerce.banner.api;

import com.luxora.commerce.banner.dto.BannerResponse;
import com.luxora.commerce.banner.dto.BannerUpsertRequest;
import com.luxora.commerce.banner.service.BannerService;
import com.luxora.commerce.catalog.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/banners")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<BannerResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(bannerService.listAdminBanners(page, size, active, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerResponse> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(bannerService.getBanner(id));
    }

    @PostMapping
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerUpsertRequest request) {
        BannerResponse created = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannerResponse> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BannerUpsertRequest request
    ) {
        return ResponseEntity.ok(bannerService.updateBanner(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BannerResponse> toggleStatus(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(bannerService.toggleBannerStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
