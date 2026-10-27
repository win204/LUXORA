package com.luxora.commerce.review.dto;

import java.util.Map;

public class ProductReviewSummaryResponse {

    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingDistribution;

    public ProductReviewSummaryResponse() {
    }

    public ProductReviewSummaryResponse(double averageRating, long totalReviews, Map<Integer, Long> ratingDistribution) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
}
