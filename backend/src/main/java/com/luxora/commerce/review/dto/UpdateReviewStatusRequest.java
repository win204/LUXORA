package com.luxora.commerce.review.dto;

import com.luxora.commerce.review.model.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateReviewStatusRequest {

    @NotNull(message = "Review status is required")
    private ReviewStatus status;

    public UpdateReviewStatusRequest() {
    }

    public UpdateReviewStatusRequest(ReviewStatus status) {
        this.status = status;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }
}
