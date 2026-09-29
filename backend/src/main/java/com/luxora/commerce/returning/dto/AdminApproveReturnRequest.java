package com.luxora.commerce.returning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminApproveReturnRequest(
        @Size(max = 500) String adminNote,
        List<@Valid AdminReturnQuantityRequest> items) {
}
