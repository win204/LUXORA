package com.luxora.commerce.catalog.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 240)
    private String subtitle;

    @Column(nullable = false, length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private Set<ProductImage> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private Set<ProductSpecification> specifications = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sku ASC")
    private Set<ProductVariant> variants = new LinkedHashSet<>();

    protected Product() {
    }

    public Product(String name, String slug, String subtitle, String description, Brand brand, Category category) {
        this.name = name;
        this.slug = slug;
        this.subtitle = subtitle;
        this.description = description;
        this.brand = brand;
        this.category = category;
    }

    public void update(String name, String slug, String subtitle, String description, Brand brand, Category category, boolean active) {
        this.name = name;
        this.slug = slug;
        this.subtitle = subtitle;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.active = active;
    }

    public void addImage(String url, String altText, int displayOrder) {
        images.add(new ProductImage(this, url, altText, displayOrder));
    }

    public boolean removeImage(UUID imageId) {
        return images.removeIf(image -> image.getId().equals(imageId));
    }

    public void addSpecification(String name, String value, int displayOrder) {
        specifications.add(new ProductSpecification(this, name, value, displayOrder));
    }

    public boolean removeSpecification(UUID specificationId) {
        return specifications.removeIf(specification -> specification.getId().equals(specificationId));
    }

    public void addVariant(ProductVariant variant) {
        variant.assignProduct(this);
        variants.add(variant);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public Brand getBrand() {
        return brand;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }

    public Set<ProductImage> getImages() {
        return images;
    }

    public Set<ProductSpecification> getSpecifications() {
        return specifications;
    }

    public Set<ProductVariant> getVariants() {
        return variants;
    }
}
