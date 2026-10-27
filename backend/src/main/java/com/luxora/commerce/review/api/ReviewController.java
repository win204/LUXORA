package com.luxora.commerce.review.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.review.dto.ProductReviewSummaryResponse;
import com.luxora.commerce.review.dto.ReviewRequest;
import com.luxora.commerce.review.dto.ReviewResponse;
import com.luxora.commerce.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{slug}/reviews")
    public Page<ReviewResponse> getProductReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewService.getProductReviews(slug, pageable);
    }

    @GetMapping("/{slug}/reviews/summary")
    public ProductReviewSummaryResponse getProductReviewSummary(@PathVariable String slug) {
        return reviewService.getProductReviewSummary(slug);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(id, user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/reviews/me")
    public ResponseEntity<ReviewResponse> getMyReview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return reviewService.getMyReviewForProduct(id, user.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
