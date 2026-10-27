package com.luxora.commerce.banner.service;

import com.luxora.commerce.banner.dto.BannerResponse;
import com.luxora.commerce.banner.dto.BannerUpsertRequest;
import com.luxora.commerce.banner.model.Banner;
import com.luxora.commerce.banner.repository.BannerRepository;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;

    public BannerService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findActiveBanners(Instant.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BannerResponse> listAdminBanners(int page, int size, Boolean active, String search) {
        Pageable pageable = PageRequest.of(page, size);
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Banner> result = bannerRepository.findAdminPage(active, searchParam, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public BannerResponse getBanner(UUID id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BANNER_NOT_FOUND", "Banner not found with ID: " + id));
        return toResponse(banner);
    }

    public BannerResponse createBanner(BannerUpsertRequest request) {
        validateDateRange(request.startsAt(), request.endsAt());

        Banner banner = new Banner(
                request.title().trim(),
                trimNull(request.subtitle()),
                request.imageUrl().trim(),
                trimNull(request.linkUrl()),
                trimNull(request.ctaText()),
                request.displayOrder() != null ? request.displayOrder() : 0,
                request.active() != null ? request.active() : true,
                request.startsAt(),
                request.endsAt()
        );

        Banner saved = bannerRepository.save(banner);
        return toResponse(saved);
    }

    public BannerResponse updateBanner(UUID id, BannerUpsertRequest request) {
        validateDateRange(request.startsAt(), request.endsAt());

        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BANNER_NOT_FOUND", "Banner not found with ID: " + id));

        banner.update(
                request.title().trim(),
                trimNull(request.subtitle()),
                request.imageUrl().trim(),
                trimNull(request.linkUrl()),
                trimNull(request.ctaText()),
                request.displayOrder() != null ? request.displayOrder() : 0,
                request.active() != null ? request.active() : true,
                request.startsAt(),
                request.endsAt()
        );

        return toResponse(banner);
    }

    public BannerResponse toggleBannerStatus(UUID id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BANNER_NOT_FOUND", "Banner not found with ID: " + id));
        banner.setActive(!banner.isActive());
        return toResponse(banner);
    }

    public void deleteBanner(UUID id) {
        if (!bannerRepository.existsById(id)) {
            throw new NotFoundException("BANNER_NOT_FOUND", "Banner not found with ID: " + id);
        }
        bannerRepository.deleteById(id);
    }

    private void validateDateRange(Instant start, Instant end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BadRequestException("BANNER_DATE_RANGE_INVALID", "Banner endsAt must be after startsAt");
        }
    }

    private String trimNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    public BannerResponse toResponse(Banner banner) {
        return new BannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getSubtitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getCtaText(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getStartsAt(),
                banner.getEndsAt(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }
}
