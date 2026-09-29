package com.luxora.commerce.catalog.service;

import com.luxora.commerce.catalog.dto.BrandResponse;
import com.luxora.commerce.catalog.dto.CategoryResponse;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.catalog.dto.ProductDetailResponse;
import com.luxora.commerce.catalog.dto.ProductListResponse;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.repository.BrandRepository;
import com.luxora.commerce.catalog.repository.CategoryRepository;
import com.luxora.commerce.catalog.repository.ProductListMeta;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CatalogMapper mapper;

    public CatalogQueryService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            CatalogMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.mapper = mapper;
    }

    public PageResponse<ProductListResponse> findProducts(
            int page,
            int size,
            String sort,
            String search,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("INVALID_PRICE_RANGE", "minPrice must be less than or equal to maxPrice");
        }

        Pageable pageable = PageRequest.of(page, size);
        String cleanSearch = clean(search);
        Page<Product> products = productRepository.findProducts(
                cleanSearch != null,
                cleanSearch == null ? "" : "%" + cleanSearch.toLowerCase() + "%",
                clean(category),
                clean(brand),
                minPrice,
                maxPrice,
                sort,
                pageable);
        Map<UUID, ProductListMeta> metaByProductId = products.isEmpty()
                ? Collections.emptyMap()
                : productRepository.findListMeta(products.getContent().stream().map(Product::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(ProductListMeta::productId, Function.identity()));

        return new PageResponse<>(
                products.getContent().stream()
                        .map(product -> mapper.toProductListResponse(
                                product,
                                metaByProductId.getOrDefault(product.getId(), new ProductListMeta(product.getId(), null, 0))))
                        .toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages());
    }

    public ProductDetailResponse findProductBySlug(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug)
                .map(mapper::toProductDetailResponse)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    public java.util.List<CategoryResponse> findCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toCategoryResponse)
                .toList();
    }

    public java.util.List<BrandResponse> findBrands() {
        return brandRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toBrandResponse)
                .toList();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
