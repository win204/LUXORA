package com.luxora.commerce.admin.api;

import com.luxora.commerce.admin.dto.AdminDashboardResponse;
import com.luxora.commerce.admin.dto.AdminInventoryUpdateRequest;
import com.luxora.commerce.admin.dto.AdminOrderDetailResponse;
import com.luxora.commerce.admin.dto.AdminOrderNoteRequest;
import com.luxora.commerce.admin.dto.AdminOrderListResponse;
import com.luxora.commerce.admin.dto.AdminOrderStatusUpdateRequest;
import com.luxora.commerce.admin.dto.AdminProductDetailResponse;
import com.luxora.commerce.admin.dto.AdminProductImageRequest;
import com.luxora.commerce.admin.dto.AdminProductListResponse;
import com.luxora.commerce.admin.dto.AdminProductSpecificationRequest;
import com.luxora.commerce.admin.dto.AdminProductUpsertRequest;
import com.luxora.commerce.admin.dto.AdminProductVariantResponse;
import com.luxora.commerce.admin.dto.AdminVariantCreateRequest;
import com.luxora.commerce.admin.dto.AdminVariantUpdateRequest;
import com.luxora.commerce.admin.service.AdminService;
import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.refund.dto.RefundAndCancelRequest;
import com.luxora.commerce.returning.dto.AdminApproveReturnRequest;
import com.luxora.commerce.returning.dto.AdminReturnNoteRequest;
import com.luxora.commerce.returning.dto.AdminReceiveReturnRequest;
import com.luxora.commerce.returning.dto.AdminRejectReturnRequest;
import com.luxora.commerce.returning.dto.ReturnRefundRequest;
import com.luxora.commerce.returning.dto.ReturnResponse;
import com.luxora.commerce.returning.dto.ReturnSummaryResponse;
import com.luxora.commerce.returning.model.ReturnStatus;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.shipment.dto.ShipmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard", description = "Returns simple operational counts for products, variants, and orders.")
    AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/products")
    @Operation(summary = "Admin products", description = "Returns a paginated product list for admin review.")
    PageResponse<AdminProductListResponse> products(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return adminService.products(page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create admin product", description = "Creates a product shell with unique slug and brand/category assignment.")
    AdminProductDetailResponse createProduct(@Valid @RequestBody AdminProductUpsertRequest request) {
        return adminService.createProduct(request);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Admin product detail", description = "Returns product detail with variants, images, specifications, and inventory quantity for admin review.")
    AdminProductDetailResponse product(@PathVariable UUID id) {
        return adminService.product(id);
    }

    @PatchMapping("/products/{id}")
    @Operation(summary = "Update admin product", description = "Updates product fields while preserving unique slug constraints.")
    AdminProductDetailResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody AdminProductUpsertRequest request) {
        return adminService.updateProduct(id, request);
    }

    @PostMapping("/products/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product variant", description = "Adds a purchasable SKU and initial inventory quantity to a product.")
    AdminProductDetailResponse createVariant(@PathVariable UUID id, @Valid @RequestBody AdminVariantCreateRequest request) {
        return adminService.createVariant(id, request);
    }

    @PatchMapping("/variants/{id}")
    @Operation(summary = "Update product variant", description = "Updates SKU, attributes, price, and active state while preserving SKU uniqueness.")
    AdminProductDetailResponse updateVariant(@PathVariable UUID id, @Valid @RequestBody AdminVariantUpdateRequest request) {
        return adminService.updateVariant(id, request);
    }

    @PatchMapping("/inventory/{variantId}")
    @Operation(summary = "Update inventory", description = "Updates available inventory quantity for a product variant.")
    AdminProductVariantResponse updateInventory(@PathVariable UUID variantId, @Valid @RequestBody AdminInventoryUpdateRequest request) {
        return adminService.updateInventory(variantId, request);
    }

    @PostMapping("/products/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add product image", description = "Adds an image URL to a product.")
    AdminProductDetailResponse addImage(@PathVariable UUID id, @Valid @RequestBody AdminProductImageRequest request) {
        return adminService.addImage(id, request);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @Operation(summary = "Remove product image", description = "Removes an image from a product.")
    AdminProductDetailResponse removeImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return adminService.removeImage(id, imageId);
    }

    @PostMapping("/products/{id}/specifications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add product specification", description = "Adds a specification row to a product.")
    AdminProductDetailResponse addSpecification(@PathVariable UUID id, @Valid @RequestBody AdminProductSpecificationRequest request) {
        return adminService.addSpecification(id, request);
    }

    @DeleteMapping("/products/{id}/specifications/{specId}")
    @Operation(summary = "Remove product specification", description = "Removes a specification from a product.")
    AdminProductDetailResponse removeSpecification(@PathVariable UUID id, @PathVariable UUID specId) {
        return adminService.removeSpecification(id, specId);
    }


    @GetMapping("/returns")
    @Operation(summary = "Admin returns", description = "Returns a paginated return queue with status, order, customer, tracking, date, and safe sort filters.")
    PageResponse<ReturnSummaryResponse> returns(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) @Size(max = 254) String customerEmail,
            @RequestParam(required = false) @Size(max = 120) String trackingNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) @Size(max = 40) String sort) {
        return adminService.returns(page, size, status, orderId, customerEmail, trackingNumber, dateFrom, dateTo, sort);
    }

    @GetMapping("/returns/{id}")
    @Operation(summary = "Admin return detail", description = "Returns a safe return request detail for admin review.")
    ReturnResponse returnDetail(@PathVariable UUID id) {
        return adminService.returnDetail(id);
    }

    @PatchMapping("/returns/{id}/note")
    @Operation(summary = "Update return note", description = "Sets or clears the admin operational note without changing the return lifecycle.")
    ReturnResponse updateReturnNote(@PathVariable UUID id, @Valid @RequestBody AdminReturnNoteRequest request) {
        return adminService.updateReturnNote(id, request);
    }

    @PostMapping("/returns/{id}/approve")
    @Operation(summary = "Approve return", description = "Approves requested return quantities.")
    ReturnResponse approveReturn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminApproveReturnRequest request) {
        return adminService.approveReturn(id, user.id(), request);
    }

    @PostMapping("/returns/{id}/reject")
    @Operation(summary = "Reject return", description = "Rejects a requested return.")
    ReturnResponse rejectReturn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminRejectReturnRequest request) {
        return adminService.rejectReturn(id, user.id(), request);
    }

    @PostMapping("/returns/{id}/receive")
    @Operation(summary = "Receive return", description = "Marks approved returned goods as received and restocks accepted quantities.")
    ReturnResponse receiveReturn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminReceiveReturnRequest request) {
        return adminService.receiveReturn(id, user.id(), request);
    }

    @PostMapping("/returns/{id}/refund")
    @Operation(summary = "Refund return", description = "Creates a mock refund for a received return and marks it refunded only on success.")
    ReturnResponse refundReturn(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ReturnRefundRequest request) {
        return adminService.refundReturn(id, user.id(), request);
    }
    @PostMapping("/returns/{id}/shipping-label")
    @Operation(summary = "Generate return label", description = "Generates idempotent local mock return tracking and label metadata for an approved return.")
    ReturnResponse generateReturnShippingLabel(@PathVariable UUID id) {
        return adminService.generateReturnShippingLabel(id);
    }

    @PostMapping("/returns/{id}/mark-received")
    @Operation(summary = "Mark return received", description = "Receives an approved return with shipment metadata and restocks accepted quantities.")
    ReturnResponse markReturnReceived(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminReceiveReturnRequest request) {
        return adminService.markReturnReceived(id, user.id(), request);
    }
    @GetMapping("/orders")
    @Operation(summary = "Admin orders", description = "Returns a paginated order queue with status, customer, tracking, date, and safe sort filters.")
    PageResponse<AdminOrderListResponse> orders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) @Size(max = 254) String customerEmail,
            @RequestParam(required = false) @Size(max = 120) String trackingNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) @Size(max = 40) String sort) {
        return adminService.orders(page, size, status, orderId, customerEmail, trackingNumber, dateFrom, dateTo, sort);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Admin order detail", description = "Returns a safe order snapshot across customers for admin review.")
    AdminOrderDetailResponse order(@PathVariable UUID id) {
        return adminService.order(id);
    }

    @PatchMapping("/orders/{id}/note")
    @Operation(summary = "Update order note", description = "Sets or clears the admin fulfillment note without changing the order lifecycle.")
    AdminOrderDetailResponse updateOrderNote(@PathVariable UUID id, @Valid @RequestBody AdminOrderNoteRequest request) {
        return adminService.updateOrderNote(id, request);
    }
    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel admin order", description = "Cancels a pending order and restores inventory.")
    AdminOrderDetailResponse cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        return adminService.cancelOrder(id, user.id());
    }
    @PostMapping("/orders/{id}/refund-and-cancel")
    @Operation(summary = "Refund and cancel admin order", description = "Creates a mock refund attempt and cancels a paid or processing order only when the refund succeeds.")
    AdminOrderDetailResponse refundAndCancelOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RefundAndCancelRequest request) {
        return adminService.refundAndCancelOrder(id, user.id(), request);
    }
    @PostMapping("/orders/{id}/shipment")
    @Operation(summary = "Create order shipment", description = "Creates shipment metadata and marks a processing order as shipped.")
    AdminOrderDetailResponse createShipment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ShipmentRequest request) {
        return adminService.createShipment(id, user.id(), request);
    }

    @PatchMapping("/orders/{id}/shipment")
    @Operation(summary = "Update order shipment", description = "Updates carrier and tracking metadata while an order is shipped.")
    AdminOrderDetailResponse updateShipment(
            @PathVariable UUID id,
            @Valid @RequestBody ShipmentRequest request) {
        return adminService.updateShipment(id, request);
    }
    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Update admin order status", description = "Applies a controlled admin order status transition and records history.")
    AdminOrderDetailResponse updateOrderStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AdminOrderStatusUpdateRequest request) {
        return adminService.updateOrderStatus(id, user.id(), request);
    }
}


