package com.luxora.commerce.review.dto;

import com.luxora.commerce.review.model.ReviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminReviewResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String productSlug;
    private UUID userId;
    private String userEmail;
    private String userName;
    private int rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private boolean isVerifiedPurchase;
    private List<String> imageUrls;
    private Instant createdAt;
    private Instant updatedAt;

    public AdminReviewResponse() {
    }

    public AdminReviewResponse(UUID id, UUID productId, String productName, String productSlug,
                               UUID userId, String userEmail, String userName, int rating,
                               String title, String comment, ReviewStatus status, boolean isVerifiedPurchase,
                               List<String> imageUrls, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.isVerifiedPurchase = isVerifiedPurchase;
        this.imageUrls = imageUrls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSlug() {
        return productSlug;
    }

    public void setProductSlug(String productSlug) {
        this.productSlug = productSlug;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
