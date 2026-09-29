package com.luxora.commerce.returning.service;

import com.luxora.commerce.returning.dto.ReturnItemResponse;
import com.luxora.commerce.returning.dto.ReturnResponse;
import com.luxora.commerce.returning.dto.ReturnShipmentResponse;
import com.luxora.commerce.returning.dto.ReturnStatusHistoryResponse;
import com.luxora.commerce.returning.dto.ReturnSummaryResponse;
import com.luxora.commerce.returning.model.ReturnItem;
import com.luxora.commerce.returning.model.ReturnRequest;
import com.luxora.commerce.returning.model.ReturnStatusHistory;
import com.luxora.commerce.user.model.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReturnMapper {

    public ReturnSummaryResponse toSummary(ReturnRequest request) {
        return new ReturnSummaryResponse(
                request.getId(),
                request.getOrder().getId(),
                request.getUser().getEmail(),
                request.getStatus().name(),
                request.getItems().stream().mapToInt(ReturnItem::getRequestedQuantity).sum(),
                request.getItems().stream().mapToInt(ReturnItem::getApprovedQuantity).sum(),
                request.getItems().stream().mapToInt(ReturnItem::getReceivedQuantity).sum(),
                estimatedRefund(request),
                request.getOrder().getCurrency(),
                request.getShipment() == null ? null : request.getShipment().getTrackingNumber(),
                request.getRequestedAt(),
                request.getUpdatedAt());
    }

    public ReturnResponse toResponse(ReturnRequest request, List<ReturnStatusHistory> history) {
        User user = request.getUser();
        return new ReturnResponse(
                request.getId(),
                request.getOrder().getId(),
                user.getId(),
                user.getEmail(),
                request.getStatus().name(),
                request.getCustomerNote(),
                request.getAdminNote(),
                request.getOrder().getCurrency(),
                estimatedRefund(request),
                receivedRefundAmount(request),
                request.getRequestedAt(),
                request.getApprovedAt(),
                request.getRejectedAt(),
                request.getReceivedAt(),
                request.getRefundedAt(),
                request.getCancelledAt(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                toShipmentResponse(request),
                request.getItems().stream().map(this::toItemResponse).toList(),
                history.stream().map(this::toHistoryResponse).toList());
    }


    private ReturnShipmentResponse toShipmentResponse(ReturnRequest request) {
        if (request.getShipment() == null) {
            return null;
        }
        return new ReturnShipmentResponse(
                request.getShipment().getCarrier(),
                request.getShipment().getTrackingNumber(),
                request.getShipment().getMockLabelReference(),
                request.getShipment().getShippedAt(),
                request.getShipment().getReceivedAt());
    }
    private ReturnItemResponse toItemResponse(ReturnItem item) {
        return new ReturnItemResponse(
                item.getId(),
                item.getOrderItem().getId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getOrderItem().getQuantity(),
                item.getRequestedQuantity(),
                item.getApprovedQuantity(),
                item.getReceivedQuantity(),
                item.getReason());
    }

    private ReturnStatusHistoryResponse toHistoryResponse(ReturnStatusHistory history) {
        User changedBy = history.getChangedByUser();
        return new ReturnStatusHistoryResponse(
                history.getId(),
                history.getFromStatus().name(),
                history.getToStatus().name(),
                history.getChangedAt(),
                changedBy == null ? null : changedBy.getId(),
                changedBy == null ? null : changedBy.getEmail());
    }

    private BigDecimal estimatedRefund(ReturnRequest request) {
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getApprovedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal receivedRefundAmount(ReturnRequest request) {
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getReceivedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
