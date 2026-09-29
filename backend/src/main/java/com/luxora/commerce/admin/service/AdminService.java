package com.luxora.commerce.admin.service;

import com.luxora.commerce.admin.dto.AdminDashboardResponse;
import com.luxora.commerce.admin.dto.AdminInventoryUpdateRequest;
import com.luxora.commerce.admin.dto.AdminOrderDetailResponse;
import com.luxora.commerce.admin.dto.AdminOrderNoteRequest;
import com.luxora.commerce.admin.dto.AdminOrderListResponse;
import com.luxora.commerce.admin.dto.AdminOrderStatusHistoryResponse;
import com.luxora.commerce.admin.dto.AdminOrderStatusUpdateRequest;
import com.luxora.commerce.admin.dto.AdminPaymentSummaryResponse;
import com.luxora.commerce.admin.dto.AdminProductDetailResponse;
import com.luxora.commerce.admin.dto.AdminProductImageRequest;
import com.luxora.commerce.admin.dto.AdminProductImageResponse;
import com.luxora.commerce.admin.dto.AdminProductListResponse;
import com.luxora.commerce.admin.dto.AdminProductSpecificationRequest;
import com.luxora.commerce.admin.dto.AdminProductSpecificationResponse;
import com.luxora.commerce.admin.dto.AdminProductUpsertRequest;
import com.luxora.commerce.admin.dto.AdminProductVariantResponse;
import com.luxora.commerce.admin.dto.AdminRefundSummaryResponse;
import com.luxora.commerce.admin.dto.AdminVariantCreateRequest;
import com.luxora.commerce.admin.dto.AdminVariantUpdateRequest;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.catalog.model.Brand;
import com.luxora.commerce.catalog.model.Category;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductImage;
import com.luxora.commerce.catalog.model.ProductSpecification;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.AdminProductListMeta;
import com.luxora.commerce.catalog.repository.BrandRepository;
import com.luxora.commerce.catalog.repository.CategoryRepository;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.inventory.model.InventoryItem;
import com.luxora.commerce.order.dto.OrderItemResponse;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.model.OrderStatusHistory;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.order.repository.OrderStatusHistoryRepository;
import com.luxora.commerce.order.service.OrderCancellationService;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.repository.PaymentRepository;
import com.luxora.commerce.order.shipment.dto.ShipmentRequest;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import com.luxora.commerce.order.shipment.model.Shipment;
import com.luxora.commerce.order.shipment.service.ShipmentService;
import com.luxora.commerce.refund.dto.RefundAndCancelRequest;
import com.luxora.commerce.refund.model.Refund;
import com.luxora.commerce.refund.repository.RefundRepository;
import com.luxora.commerce.refund.service.RefundService;
import com.luxora.commerce.returning.dto.AdminApproveReturnRequest;
import com.luxora.commerce.returning.dto.AdminReturnNoteRequest;
import com.luxora.commerce.returning.dto.AdminReceiveReturnRequest;
import com.luxora.commerce.returning.dto.AdminRejectReturnRequest;
import com.luxora.commerce.returning.dto.ReturnRefundRequest;
import com.luxora.commerce.returning.dto.ReturnResponse;
import com.luxora.commerce.returning.dto.ReturnSummaryResponse;
import com.luxora.commerce.returning.model.ReturnStatus;
import com.luxora.commerce.returning.service.ReturnService;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final OrderCancellationService orderCancellationService;
    private final RefundService refundService;
    private final ShipmentService shipmentService;
    private final ReturnService returnService;

    public AdminService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            UserRepository userRepository,
            OrderCancellationService orderCancellationService,
            RefundService refundService,
            ShipmentService shipmentService,
            ReturnService returnService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
        this.orderCancellationService = orderCancellationService;
        this.refundService = refundService;
        this.shipmentService = shipmentService;
        this.returnService = returnService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                productRepository.count(),
                productVariantRepository.count(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductListResponse> products(int page, int size) {
        Page<Product> productPage = productRepository.findAdminPage(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        List<UUID> productIds = productPage.getContent().stream().map(Product::getId).toList();
        Map<UUID, AdminProductListMeta> metaByProductId = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAdminListMeta(productIds).stream()
                        .collect(Collectors.toMap(AdminProductListMeta::productId, Function.identity()));
        List<AdminProductListResponse> content = productPage.getContent().stream()
                .map(product -> toProductListResponse(product, metaByProductId.get(product.getId())))
                .toList();
        return new PageResponse<>(content, productPage.getNumber(), productPage.getSize(), productPage.getTotalElements(), productPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminProductDetailResponse product(UUID id) {
        return toProductDetailResponse(findProduct(id));
    }

    @Transactional
    public AdminProductDetailResponse createProduct(AdminProductUpsertRequest request) {
        String slug = normalizeSlug(request.slug());
        if (productRepository.existsBySlug(slug)) {
            throw new ConflictException("PRODUCT_SLUG_CONFLICT", "Product slug already exists");
        }
        Brand brand = findBrand(request.brandId());
        Category category = findCategory(request.categoryId());
        Product product = new Product(trim(request.name()), slug, trimToNull(request.subtitle()), trim(request.description()), brand, category);
        product.update(trim(request.name()), slug, trimToNull(request.subtitle()), trim(request.description()), brand, category, request.active());
        return toProductDetailResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminProductDetailResponse updateProduct(UUID id, AdminProductUpsertRequest request) {
        Product product = findProduct(id);
        String slug = normalizeSlug(request.slug());
        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("PRODUCT_SLUG_CONFLICT", "Product slug already exists");
        }
        product.update(
                trim(request.name()),
                slug,
                trimToNull(request.subtitle()),
                trim(request.description()),
                findBrand(request.brandId()),
                findCategory(request.categoryId()),
                request.active());
        return toProductDetailResponse(product);
    }

    @Transactional
    public AdminProductDetailResponse createVariant(UUID productId, AdminVariantCreateRequest request) {
        Product product = findProduct(productId);
        String sku = normalizeSku(request.sku());
        if (productVariantRepository.existsBySku(sku)) {
            throw new ConflictException("VARIANT_SKU_CONFLICT", "Variant SKU already exists");
        }
        ProductVariant variant = new ProductVariant(sku, trimToNull(request.color()), trimToNull(request.storage()), request.price(), request.quantityAvailable());
        variant.update(sku, trimToNull(request.color()), trimToNull(request.storage()), request.price(), request.active());
        product.addVariant(variant);
        productRepository.saveAndFlush(product);
        return toProductDetailResponse(product);
    }

    @Transactional
    public AdminProductDetailResponse updateVariant(UUID id, AdminVariantUpdateRequest request) {
        ProductVariant variant = findVariant(id);
        String sku = normalizeSku(request.sku());
        if (productVariantRepository.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("VARIANT_SKU_CONFLICT", "Variant SKU already exists");
        }
        variant.update(sku, trimToNull(request.color()), trimToNull(request.storage()), request.price(), request.active());
        return toProductDetailResponse(findProduct(variant.getProduct().getId()));
    }

    @Transactional
    public AdminProductVariantResponse updateInventory(UUID variantId, AdminInventoryUpdateRequest request) {
        ProductVariant variant = findVariant(variantId);
        variant.getInventoryItem().updateQuantity(request.quantityAvailable());
        return toVariantResponse(variant);
    }

    @Transactional
    public AdminProductDetailResponse addImage(UUID productId, AdminProductImageRequest request) {
        Product product = findProduct(productId);
        product.addImage(trim(request.url()), trimToNull(request.altText()), request.displayOrder());
        productRepository.saveAndFlush(product);
        return toProductDetailResponse(product);
    }

    @Transactional
    public AdminProductDetailResponse removeImage(UUID productId, UUID imageId) {
        Product product = findProduct(productId);
        if (!product.removeImage(imageId)) {
            throw new NotFoundException("PRODUCT_IMAGE_NOT_FOUND", "Product image not found");
        }
        return toProductDetailResponse(product);
    }

    @Transactional
    public AdminProductDetailResponse addSpecification(UUID productId, AdminProductSpecificationRequest request) {
        Product product = findProduct(productId);
        product.addSpecification(trim(request.name()), trim(request.value()), request.displayOrder());
        productRepository.saveAndFlush(product);
        return toProductDetailResponse(product);
    }

    @Transactional
    public AdminProductDetailResponse removeSpecification(UUID productId, UUID specId) {
        Product product = findProduct(productId);
        if (!product.removeSpecification(specId)) {
            throw new NotFoundException("PRODUCT_SPECIFICATION_NOT_FOUND", "Product specification not found");
        }
        return toProductDetailResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnSummaryResponse> returns(
            int page,
            int size,
            ReturnStatus status,
            UUID orderId,
            String customerEmail,
            String trackingNumber,
            Instant dateFrom,
            Instant dateTo,
            String sort) {
        return returnService.listAdminReturns(page, size, status, orderId, customerEmail, trackingNumber, dateFrom, dateTo, sort);
    }

    @Transactional(readOnly = true)
    public ReturnResponse returnDetail(UUID returnId) {
        return returnService.getAdminReturn(returnId);
    }

    @Transactional
    public ReturnResponse updateReturnNote(UUID returnId, AdminReturnNoteRequest request) {
        return returnService.updateAdminNote(returnId, request);
    }

    @Transactional
    public ReturnResponse approveReturn(UUID returnId, UUID adminUserId, AdminApproveReturnRequest request) {
        return returnService.approve(returnId, adminUserId, request);
    }

    @Transactional
    public ReturnResponse rejectReturn(UUID returnId, UUID adminUserId, AdminRejectReturnRequest request) {
        return returnService.reject(returnId, adminUserId, request);
    }

    @Transactional
    public ReturnResponse receiveReturn(UUID returnId, UUID adminUserId, AdminReceiveReturnRequest request) {
        return returnService.receive(returnId, adminUserId, request);
    }

    @Transactional
    public ReturnResponse refundReturn(UUID returnId, UUID adminUserId, ReturnRefundRequest request) {
        return returnService.refund(returnId, adminUserId, request);
    }
    @Transactional
    public ReturnResponse generateReturnShippingLabel(UUID returnId) {
        return returnService.generateShippingLabel(returnId);
    }

    @Transactional
    public ReturnResponse markReturnReceived(UUID returnId, UUID adminUserId, AdminReceiveReturnRequest request) {
        return returnService.markReceived(returnId, adminUserId, request);
    }
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderListResponse> orders(
            int page,
            int size,
            OrderStatus status,
            UUID orderId,
            String customerEmail,
            String trackingNumber,
            Instant dateFrom,
            Instant dateTo,
            String sort) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("ORDER_DATE_RANGE_INVALID", "dateFrom must be before or equal to dateTo");
        }
        Page<Order> orderPage = orderRepository.findAdminPage(
                status,
                orderId,
                trimToNull(customerEmail),
                trimToNull(trackingNumber),
                dateFrom,
                dateTo,
                PageRequest.of(page, size, parseOrderSort(sort)));
        List<UUID> ids = orderPage.getContent().stream().map(Order::getId).toList();
        Map<UUID, Order> ordersById = ids.isEmpty()
                ? Map.of()
                : orderRepository.findAdminSummariesByIdIn(ids).stream()
                        .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<AdminOrderListResponse> content = ids.stream().map(ordersById::get).map(this::toOrderListResponse).toList();
        return new PageResponse<>(content, orderPage.getNumber(), orderPage.getSize(), orderPage.getTotalElements(), orderPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse order(UUID id) {
        Order order = orderRepository.findAdminById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        return toOrderDetailResponse(order);
    }

    @Transactional
    public AdminOrderDetailResponse updateOrderNote(UUID orderId, AdminOrderNoteRequest request) {
        Order order = orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        order.updateAdminNote(trimToNull(request.note()));
        return toOrderDetailResponse(order);
    }
    @Transactional
    public AdminOrderDetailResponse cancelOrder(UUID orderId, UUID adminUserId) {
        return toOrderDetailResponse(orderCancellationService.cancelAdminOrder(orderId, adminUserId));
    }

    @Transactional
    public AdminOrderDetailResponse refundAndCancelOrder(UUID orderId, UUID adminUserId, RefundAndCancelRequest request) {
        return toOrderDetailResponse(refundService.refundAndCancel(orderId, adminUserId, request));
    }

    @Transactional
    public AdminOrderDetailResponse createShipment(UUID orderId, UUID adminUserId, ShipmentRequest request) {
        return toOrderDetailResponse(shipmentService.createShipment(orderId, adminUserId, request));
    }

    @Transactional
    public AdminOrderDetailResponse updateShipment(UUID orderId, ShipmentRequest request) {
        return toOrderDetailResponse(shipmentService.updateShipment(orderId, request));
    }

    @Transactional
    public AdminOrderDetailResponse updateOrderStatus(UUID orderId, UUID adminUserId, AdminOrderStatusUpdateRequest request) {
        if (request.status() == OrderStatus.DELIVERED) {
            return toOrderDetailResponse(shipmentService.markDelivered(orderId, adminUserId));
        }
        Order order = orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();
        if (!isAllowedTransition(current, target)) {
            throw new ConflictException("ORDER_STATUS_TRANSITION_INVALID", "Order status transition is not allowed");
        }
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Admin user not found"));
        order.changeStatus(target);
        orderStatusHistoryRepository.save(new OrderStatusHistory(order, current, target, adminUser));
        return toOrderDetailResponse(order);
    }

    private Product findProduct(UUID id) {
        return productRepository.findAdminDetailById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    private ProductVariant findVariant(UUID id) {
        return productVariantRepository.findAdminById(id)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));
    }

    private Brand findBrand(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BRAND_NOT_FOUND", "Brand not found"));
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
    }

    private AdminProductListResponse toProductListResponse(Product product, AdminProductListMeta meta) {
        return new AdminProductListResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBrand().getName(),
                product.getCategory().getName(),
                product.isActive(),
                meta == null ? 0 : Math.toIntExact(meta.variantCount()),
                meta == null ? null : meta.minPrice(),
                meta != null && meta.inStockVariantCount() > 0);
    }

    private AdminProductDetailResponse toProductDetailResponse(Product product) {
        return new AdminProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSubtitle(),
                product.getDescription(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.isActive(),
                product.getImages().stream()
                        .sorted(Comparator.comparingInt(ProductImage::getDisplayOrder))
                        .map(this::toImageResponse)
                        .toList(),
                product.getSpecifications().stream()
                        .sorted(Comparator.comparingInt(ProductSpecification::getDisplayOrder))
                        .map(this::toSpecificationResponse)
                        .toList(),
                product.getVariants().stream()
                        .sorted(Comparator.comparing(ProductVariant::getSku))
                        .map(this::toVariantResponse)
                        .toList());
    }

    private AdminProductImageResponse toImageResponse(ProductImage image) {
        return new AdminProductImageResponse(image.getId(), image.getUrl(), image.getAltText(), image.getDisplayOrder());
    }

    private AdminProductSpecificationResponse toSpecificationResponse(ProductSpecification specification) {
        return new AdminProductSpecificationResponse(specification.getId(), specification.getName(), specification.getValue(), specification.getDisplayOrder());
    }

    private AdminProductVariantResponse toVariantResponse(ProductVariant variant) {
        InventoryItem inventory = variant.getInventoryItem();
        int quantity = inventory == null ? 0 : inventory.getQuantityAvailable();
        return new AdminProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                variant.getPrice(),
                variant.isActive(),
                variant.isActive() && quantity > 0,
                quantity);
    }

    private AdminOrderListResponse toOrderListResponse(Order order) {
        return new AdminOrderListResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getStatus().name(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getShipment() == null ? null : order.getShipment().getTrackingNumber(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private AdminOrderDetailResponse toOrderDetailResponse(Order order) {
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getStatus().name(),
                order.getAdminNote(),
                order.getItems().stream().map(this::toOrderItemResponse).toList(),
                new ShippingAddressResponse(
                        order.getRecipientName(),
                        order.getPhone(),
                        order.getAddressLine1(),
                        order.getAddressLine2(),
                        order.getCity(),
                        order.getProvince(),
                        order.getCountry(),
                        order.getPostalCode()),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTax(),
                order.getDiscount(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                paymentRepository.findTopByOrder_IdOrderByCreatedAtDesc(order.getId()).map(this::toPaymentSummaryResponse).orElse(null),
                refundRepository.findTopByOrder_IdOrderByCreatedAtDesc(order.getId()).map(this::toRefundSummaryResponse).orElse(null),
                toShipmentResponse(order.getShipment()),
                orderStatusHistoryRepository.findByOrder_IdOrderByChangedAtAsc(order.getId()).stream()
                        .map(this::toStatusHistoryResponse)
                        .toList());
    }


    private Sort parseOrderSort(String sort) {
        String value = trimToNull(sort);
        if (value == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || (!parts[0].equals("createdAt") && !parts[0].equals("updatedAt"))) {
            throw new BadRequestException("ORDER_SORT_INVALID", "sort must be createdAt,asc|desc or updatedAt,asc|desc");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("ORDER_SORT_INVALID", "sort must be createdAt,asc|desc or updatedAt,asc|desc");
        }
        return Sort.by(direction, parts[0]);
    }
    private boolean isAllowedTransition(OrderStatus current, OrderStatus target) {
        return current == OrderStatus.PAID && target == OrderStatus.PROCESSING;
    }

    private AdminPaymentSummaryResponse toPaymentSummaryResponse(Payment payment) {
        return new AdminPaymentSummaryResponse(
                payment.getId(),
                payment.getProvider(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    private AdminRefundSummaryResponse toRefundSummaryResponse(Refund refund) {
        return new AdminRefundSummaryResponse(
                refund.getId(),
                refund.getProvider(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus().name(),
                refund.getReason(),
                refund.getCreatedAt(),
                refund.getUpdatedAt());
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        return new ShipmentResponse(
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt());
    }
    private AdminOrderStatusHistoryResponse toStatusHistoryResponse(OrderStatusHistory history) {
        User changedByUser = history.getChangedByUser();
        return new AdminOrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus().name(),
                history.getToStatus().name(),
                history.getChangedAt(),
                changedByUser == null ? null : changedByUser.getId(),
                changedByUser == null ? null : changedByUser.getEmail());
    }
    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductSlug(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getColor(),
                item.getStorage(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }

    private String normalizeSlug(String value) {
        return trim(value).toLowerCase();
    }

    private String normalizeSku(String value) {
        return trim(value).toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }
}






