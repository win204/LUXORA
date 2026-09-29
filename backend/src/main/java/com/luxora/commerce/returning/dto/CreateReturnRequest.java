package com.luxora.commerce.returning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReturnRequest(
        @Size(max = 500) String customerNote,
        @NotEmpty List<@Valid CreateReturnItemRequest> items) {
}
