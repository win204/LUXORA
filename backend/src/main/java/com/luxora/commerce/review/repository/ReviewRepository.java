package com.luxora.commerce.review.repository;

import com.luxora.commerce.review.model.Review;
import com.luxora.commerce.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(CAST(r.rating as double)) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ReviewStatus status);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = :status GROUP BY r.rating")
    List<Object[]> countRatingsByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ReviewStatus status);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);
}
