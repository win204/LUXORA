package com.luxora.commerce.review.repository;

import com.luxora.commerce.review.model.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(UUID reviewId);
}
