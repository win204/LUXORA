package com.luxora.commerce.review.dto;

import com.luxora.commerce.review.model.ReviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private UUID userId;
    private String userName;
    private int rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private boolean isVerifiedPurchase;
    private List<String> imageUrls;
    private Instant createdAt;

    public ReviewResponse() {
    }

    public ReviewResponse(UUID id, UUID productId, UUID userId, String userName, int rating,
                          String title, String comment, ReviewStatus status, boolean isVerifiedPurchase,
                          List<String> imageUrls, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.isVerifiedPurchase = isVerifiedPurchase;
        this.imageUrls = imageUrls;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public boolean isVerifiedPurchase() {
        return isVerifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        isVerifiedPurchase = verifiedPurchase;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
