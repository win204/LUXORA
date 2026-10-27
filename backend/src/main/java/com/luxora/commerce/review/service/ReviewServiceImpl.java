package com.luxora.commerce.review.service;

import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.review.dto.AdminReviewResponse;
import com.luxora.commerce.review.dto.ProductReviewSummaryResponse;
import com.luxora.commerce.review.dto.ReviewRequest;
import com.luxora.commerce.review.dto.ReviewResponse;
import com.luxora.commerce.review.model.Review;
import com.luxora.commerce.review.model.ReviewImage;
import com.luxora.commerce.review.model.ReviewStatus;
import com.luxora.commerce.review.repository.ReviewRepository;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public ReviewResponse submitReview(UUID productId, UUID userId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        boolean isVerified = orderRepository.hasDeliveredOrderForProduct(userId, productId);

        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> new Review(product, user, request.getRating(), request.getTitle(), request.getComment()));

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setVerifiedPurchase(isVerified);
        review.setStatus(ReviewStatus.APPROVED);

        review.clearImages();
        if (request.getImageUrls() != null) {
            for (String url : request.getImageUrls()) {
                if (url != null && !url.trim().isEmpty()) {
                    review.addImage(url.trim());
                }
            }
        }

        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(String slug, Pageable pageable) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        return reviewRepository.findByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED, pageable)
                .map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getProductReviewSummary(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        UUID productId = product.getId();
        long totalReviews = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distribution.put(i, 0L);
        }

        if (totalReviews == 0) {
            return new ProductReviewSummaryResponse(0.0, 0L, distribution);
        }

        Double avg = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        double averageRating = (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;

        List<Object[]> counts = reviewRepository.countRatingsByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        for (Object[] row : counts) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
        }

        return new ProductReviewSummaryResponse(averageRating, totalReviews, distribution);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getMyReviewForProduct(UUID productId, UUID userId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId)
                .map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAdminReviews(ReviewStatus status, Pageable pageable) {
        Page<Review> page = (status != null)
                ? reviewRepository.findByStatus(status, pageable)
                : reviewRepository.findAll(pageable);

        return page.map(this::toAdminReviewResponse);
    }

    @Override
    public AdminReviewResponse updateReviewStatus(UUID reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found"));

        review.setStatus(status);
        Review saved = reviewRepository.save(review);
        return toAdminReviewResponse(saved);
    }

    @Override
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found"));

        reviewRepository.delete(review);
    }

    private ReviewResponse toReviewResponse(Review review) {
        String userName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
        List<String> imageUrls = review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                userName.trim(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getStatus(),
                review.isVerifiedPurchase(),
                imageUrls,
                review.getCreatedAt()
        );
    }

    private AdminReviewResponse toAdminReviewResponse(Review review) {
        String userName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
        List<String> imageUrls = review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        return new AdminReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getProduct().getSlug(),
                review.getUser().getId(),
                review.getUser().getEmail(),
                userName.trim(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getStatus(),
                review.isVerifiedPurchase(),
                imageUrls,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
