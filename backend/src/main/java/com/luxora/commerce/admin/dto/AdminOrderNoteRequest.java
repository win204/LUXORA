package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.Size;

public record AdminOrderNoteRequest(
        @Size(max = 500) String note) {
}