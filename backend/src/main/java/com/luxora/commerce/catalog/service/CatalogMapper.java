package com.luxora.commerce.catalog.service;

import com.luxora.commerce.catalog.dto.BrandResponse;
import com.luxora.commerce.catalog.dto.CategoryResponse;
import com.luxora.commerce.catalog.dto.ProductDetailResponse;
import com.luxora.commerce.catalog.dto.ProductImageResponse;
import com.luxora.commerce.catalog.dto.ProductListResponse;
import com.luxora.commerce.catalog.dto.ProductSpecificationResponse;
import com.luxora.commerce.catalog.dto.ProductVariantResponse;
import com.luxora.commerce.catalog.model.Brand;
import com.luxora.commerce.catalog.model.Category;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductListMeta;
import com.luxora.commerce.inventory.model.InventoryItem;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class CatalogMapper {

    BrandResponse toBrandResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.getSlug());
    }

    CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    ProductListResponse toProductListResponse(Product product, ProductListMeta meta) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSubtitle(),
                toBrandResponse(product.getBrand()),
                toCategoryResponse(product.getCategory()),
                meta.minPrice(),
                meta.inStock());
    }

    ProductDetailResponse toProductDetailResponse(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSubtitle(),
                product.getDescription(),
                toBrandResponse(product.getBrand()),
                toCategoryResponse(product.getCategory()),
                product.getImages().stream()
                        .map(image -> new ProductImageResponse(
                                image.getId(), image.getUrl(), image.getAltText(), image.getDisplayOrder()))
                        .toList(),
                product.getSpecifications().stream()
                        .map(specification -> new ProductSpecificationResponse(
                                specification.getId(),
                                specification.getName(),
                                specification.getValue(),
                                specification.getDisplayOrder()))
                        .toList(),
                product.getVariants().stream()
                        .filter(ProductVariant::isActive)
                        .sorted(Comparator.comparing(ProductVariant::getSku))
                        .map(this::toVariantResponse)
                        .toList());
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
        InventoryItem inventoryItem = variant.getInventoryItem();
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                variant.getPrice(),
                inventoryItem != null && inventoryItem.isAvailable());
    }
}
