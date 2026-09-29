package com.luxora.commerce.returning.dto;

import jakarta.validation.constraints.Size;

public record AdminReturnNoteRequest(
        @Size(max = 500) String note) {
}