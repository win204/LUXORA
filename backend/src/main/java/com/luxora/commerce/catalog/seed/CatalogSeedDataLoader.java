package com.luxora.commerce.catalog.seed;

import com.luxora.commerce.catalog.model.Brand;
import com.luxora.commerce.catalog.model.Category;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.BrandRepository;
import com.luxora.commerce.catalog.repository.CategoryRepository;
import com.luxora.commerce.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "luxora.seed.catalog", name = "enabled", havingValue = "true")
class CatalogSeedDataLoader implements ApplicationRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    CatalogSeedDataLoader(
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Brand aurora = brand("Aurora Devices", "aurora-devices");
        Brand atelier = brand("Atelier Sound", "atelier-sound");
        Brand lumen = brand("Lumen Works", "lumen-works");

        Category phones = category("Phones", "phones");
        Category audio = category("Audio", "audio");
        Category tablets = category("Tablets", "tablets");

        seedAeroPhoneX1(aurora, phones);
        seedStudioOne(atelier, audio);
        seedVisionSlate(lumen, tablets);
    }

    private Brand brand(String name, String slug) {
        return brandRepository.findBySlug(slug).orElseGet(() -> brandRepository.save(new Brand(name, slug)));
    }

    private Category category(String name, String slug) {
        return categoryRepository.findBySlug(slug).orElseGet(() -> categoryRepository.save(new Category(name, slug)));
    }

    private void seedAeroPhoneX1(Brand brand, Category category) {
        if (productRepository.existsBySlug("aerophone-x1")) {
            return;
        }

        Product product = new Product(
                "AeroPhone X1",
                "aerophone-x1",
                "A light, titanium smartphone tuned for everyday performance.",
                "AeroPhone X1 pairs a satin titanium frame with a bright edge-to-edge display, fast charging, and a balanced camera system for polished daily use.",
                brand,
                category);
        product.addImage("https://images.unsplash.com/photo-1598327105666-5b89351aff97", "AeroPhone X1 front and side view", 1);
        product.addImage("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9", "AeroPhone X1 on a clean surface", 2);
        product.addSpecification("Display", "6.3 inch OLED, 120Hz", 1);
        product.addSpecification("Camera", "48MP main with optical stabilization", 2);
        product.addSpecification("Battery", "All-day battery with 45W fast charging", 3);
        product.addVariant(new ProductVariant("AUR-X1-GRF-128", "Graphite", "128GB", new BigDecimal("899.00"), 18));
        product.addVariant(new ProductVariant("AUR-X1-SLV-256", "Silver", "256GB", new BigDecimal("999.00"), 9));
        product.addVariant(new ProductVariant("AUR-X1-BLU-512", "Deep Blue", "512GB", new BigDecimal("1199.00"), 0));
        productRepository.save(product);
    }

    private void seedStudioOne(Brand brand, Category category) {
        if (productRepository.existsBySlug("studio-one")) {
            return;
        }

        Product product = new Product(
                "Studio One",
                "studio-one",
                "Wireless headphones built for quiet focus and detailed listening.",
                "Studio One delivers adaptive noise control, plush memory-foam cushions, and a high-resolution acoustic profile for travel, work, and late-night listening.",
                brand,
                category);
        product.addImage("https://images.unsplash.com/photo-1505740420928-5e560c06d30e", "Studio One headphones in black", 1);
        product.addImage("https://images.unsplash.com/photo-1484704849700-f032a568e944", "Studio One headphones detail", 2);
        product.addSpecification("Audio", "Custom 40mm drivers with spatial tuning", 1);
        product.addSpecification("Noise Control", "Adaptive active noise cancellation", 2);
        product.addSpecification("Battery", "Up to 32 hours playback", 3);
        product.addVariant(new ProductVariant("ATS-SO-MBK", "Matte Black", "Standard", new BigDecimal("349.00"), 24));
        product.addVariant(new ProductVariant("ATS-SO-WHT", "Soft White", "Standard", new BigDecimal("349.00"), 15));
        productRepository.save(product);
    }

    private void seedVisionSlate(Brand brand, Category category) {
        if (productRepository.existsBySlug("vision-slate")) {
            return;
        }

        Product product = new Product(
                "Vision Slate",
                "vision-slate",
                "A thin tablet for sketching, reading, and cinematic viewing.",
                "Vision Slate combines a laminated high-brightness display, precise pen support, and desktop-class multitasking in a quiet aluminum body.",
                brand,
                category);
        product.addImage("https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0", "Vision Slate tablet display", 1);
        product.addImage("https://images.unsplash.com/photo-1561154464-82e9adf32764", "Vision Slate on desk", 2);
        product.addSpecification("Display", "11.5 inch Liquid Retina-style LCD", 1);
        product.addSpecification("Input", "Low-latency pen and keyboard support", 2);
        product.addSpecification("Connectivity", "Wi-Fi 7 with optional cellular", 3);
        product.addVariant(new ProductVariant("LMN-VS-SIL-256", "Silver", "256GB Wi-Fi", new BigDecimal("749.00"), 12));
        product.addVariant(new ProductVariant("LMN-VS-GRY-512", "Graphite", "512GB Wi-Fi + Cellular", new BigDecimal("1049.00"), 6));
        productRepository.save(product);
    }
}
