package com.luxora.commerce.catalog.repository;

import com.luxora.commerce.catalog.model.Brand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findByActiveTrueOrderByNameAsc();

    Optional<Brand> findBySlug(String slug);
}
