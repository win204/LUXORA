package com.luxora.commerce.returning.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.returning.dto.CreateReturnRequest;
import com.luxora.commerce.returning.dto.ReturnResponse;
import com.luxora.commerce.returning.dto.ReturnSummaryResponse;
import com.luxora.commerce.returning.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Returns")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/orders/{orderId}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request return", description = "Creates an item-level return request for a delivered order owned by the authenticated user.")
    ReturnResponse createReturn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateReturnRequest request) {
        return returnService.createReturn(user, orderId, request);
    }

    @GetMapping("/returns")
    @Operation(summary = "List returns", description = "Returns the authenticated user's return requests, newest first.")
    PageResponse<ReturnSummaryResponse> listReturns(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return returnService.listReturns(user, page, size);
    }

    @GetMapping("/returns/{id}")
    @Operation(summary = "Get return", description = "Returns a safe return request detail for the authenticated owner.")
    ReturnResponse getReturn(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return returnService.getReturn(user, id);
    }

    @PostMapping("/returns/{id}/cancel")
    @Operation(summary = "Cancel return", description = "Cancels a requested or approved return owned by the authenticated user.")
    ReturnResponse cancelReturn(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return returnService.cancelReturn(user, id);
    }
    @PostMapping("/returns/{id}/mark-shipped")
    @Operation(summary = "Mark return shipped", description = "Marks an approved owner return shipment as shipped after a mock label has been generated.")
    ReturnResponse markShipped(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return returnService.markShipped(user, id);
    }
}