package com.luxora.commerce.review.service;

import com.luxora.commerce.review.dto.AdminReviewResponse;
import com.luxora.commerce.review.dto.ProductReviewSummaryResponse;
import com.luxora.commerce.review.dto.ReviewRequest;
import com.luxora.commerce.review.dto.ReviewResponse;
import com.luxora.commerce.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse submitReview(UUID productId, UUID userId, ReviewRequest request);

    Page<ReviewResponse> getProductReviews(String slug, Pageable pageable);

    ProductReviewSummaryResponse getProductReviewSummary(String slug);

    Optional<ReviewResponse> getMyReviewForProduct(UUID productId, UUID userId);

    Page<AdminReviewResponse> getAdminReviews(ReviewStatus status, Pageable pageable);

    AdminReviewResponse updateReviewStatus(UUID reviewId, ReviewStatus status);

    void deleteReview(UUID reviewId);
}
