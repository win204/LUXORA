package com.luxora.commerce.banner.repository;

import com.luxora.commerce.banner.model.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    @Query("""
            SELECT b FROM Banner b
            WHERE b.active = true
              AND (b.startsAt IS NULL OR b.startsAt <= :now)
              AND (b.endsAt IS NULL OR b.endsAt >= :now)
            ORDER BY b.displayOrder ASC, b.createdAt DESC
            """)
    List<Banner> findActiveBanners(@Param("now") Instant now);

    @Query("""
            SELECT b FROM Banner b
            WHERE (:active IS NULL OR b.active = :active)
              AND (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(b.subtitle) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY b.displayOrder ASC, b.createdAt DESC
            """)
    Page<Banner> findAdminPage(
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
