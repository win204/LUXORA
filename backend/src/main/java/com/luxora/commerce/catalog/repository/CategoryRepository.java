package com.luxora.commerce.catalog.repository;

import com.luxora.commerce.catalog.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderByNameAsc();

    Optional<Category> findBySlug(String slug);
}
