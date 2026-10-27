package com.luxora.commerce.catalog.seed;

import com.luxora.commerce.catalog.model.Brand;
import com.luxora.commerce.catalog.model.Category;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.BrandRepository;
import com.luxora.commerce.catalog.repository.CategoryRepository;
import com.luxora.commerce.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.luxora.commerce.banner.model.Banner;
import com.luxora.commerce.banner.repository.BannerRepository;

@Component
@Profile({"local", "demo"})
@Order(2)
@ConditionalOnProperty(prefix = "luxora.seed.catalog", name = "extended", havingValue = "true")
class ExtendedCatalogSeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExtendedCatalogSeedDataLoader.class);

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;

    ExtendedCatalogSeedDataLoader(
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            BannerRepository bannerRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.bannerRepository = bannerRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedBanners();
        // Brands
        Brand apple = brand("Apple", "apple");
        Brand samsung = brand("Samsung", "samsung");
        Brand sony = brand("Sony", "sony");
        Brand bose = brand("Bose", "bose");
        Brand dyson = brand("Dyson", "dyson");
        Brand keychron = brand("Keychron", "keychron");
        Brand aurora = brand("Aurora Devices", "aurora-devices");
        Brand atelier = brand("Atelier Sound", "atelier-sound");
        Brand lumen = brand("Lumen Works", "lumen-works");

        // Categories
        Category phones = category("Phones", "phones");
        Category laptops = category("Laptops", "laptops");
        Category audio = category("Audio", "audio");
        Category wearables = category("Wearables", "wearables");
        Category tablets = category("Tablets", "tablets");
        Category accessories = category("Accessories", "accessories");

        // Phones (3 additional)
        seedIPhone16ProMax(apple, phones);
        seedGalaxyS25Ultra(samsung, phones);
        seedPixel9Pro(aurora, phones);

        // Laptops (4 products)
        seedMacBookPro16(apple, laptops);
        seedDellXps15(lumen, laptops);
        seedThinkPadX1(lumen, laptops);
        seedZephyrusG16(aurora, laptops);

        // Audio (4 additional)
        seedSonyWh1000Xm5(sony, audio);
        seedAirPodsMax(apple, audio);
        seedBoseQcUltra(bose, audio);
        seedDevialetMania(atelier, audio);

        // Wearables (3 products)
        seedAppleWatchUltra2(apple, wearables);
        seedGalaxyWatchUltra(samsung, wearables);
        seedGarminFenix8(lumen, wearables);

        // Tablets (2 additional)
        seedIPadPro13(apple, tablets);
        seedGalaxyTabS10(samsung, tablets);

        // Accessories (3 products)
        seedKeychronQ1Pro(keychron, accessories);
        seedAnkerPrimeStand(atelier, accessories);
        seedDysonSolarcycle(dyson, accessories);

        ensureAllProductImages();

        log.info("Extended catalog seed complete. Total products in catalog: {}", productRepository.count());
    }

    private void ensureAllProductImages() {
        attachIfEmpty("aerophone-x1", "https://images.unsplash.com/photo-1598327105666-5b89351aff97", "AeroPhone X1 front and side view");
        attachIfEmpty("studio-one", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e", "Studio One headphones in black");
        attachIfEmpty("vision-slate", "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0", "Vision Slate tablet display");
        attachIfEmpty("iphone-16-pro-max", "https://images.unsplash.com/photo-1695048133142-1a20484d2569", "iPhone 16 Pro Max Desert Titanium");
        attachIfEmpty("galaxy-s25-ultra", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf", "Galaxy S25 Ultra titanium frame");
        attachIfEmpty("pixel-9-pro", "https://images.unsplash.com/photo-1598327105666-5b89351aff97", "Pixel 9 Pro in Obsidian");
        attachIfEmpty("macbook-pro-16-m4", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8", "MacBook Pro 16 Space Black");
        attachIfEmpty("dell-xps-15-oled", "https://images.unsplash.com/photo-1593642632823-8f785ba67e45", "Dell XPS 15 display open");
        attachIfEmpty("thinkpad-x1-carbon-gen12", "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed", "ThinkPad X1 Carbon matte black");
        attachIfEmpty("rog-zephyrus-g16", "https://images.unsplash.com/photo-1603302576837-37561b2e2302", "Zephyrus G16 open on desk");
        attachIfEmpty("sony-wh-1000xm5", "https://images.unsplash.com/photo-1546435770-a3e426bf472b", "Sony WH-1000XM5 headphones");
        attachIfEmpty("airpods-max", "https://images.unsplash.com/photo-1583394838336-acd977736f90", "AirPods Max premium headphones");
        attachIfEmpty("bose-qc-ultra", "https://images.unsplash.com/photo-1572536147248-ac59a8abfa4b", "Bose QC Ultra headphones");
        attachIfEmpty("devialet-mania", "https://images.unsplash.com/photo-1545454675-3531b543be5d", "Devialet portable high-end speaker");
        attachIfEmpty("apple-watch-ultra-2", "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1", "Apple Watch Ultra 2 titanium");
        attachIfEmpty("galaxy-watch-ultra", "https://images.unsplash.com/photo-1523275335684-37898b6baf30", "Galaxy Watch Ultra smartwatch");
        attachIfEmpty("garmin-fenix-8", "https://images.unsplash.com/photo-1509042239860-f550ce710b93", "Garmin Fenix 8 multisport watch");
        attachIfEmpty("ipad-pro-13-m4", "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0", "iPad Pro 13 OLED");
        attachIfEmpty("galaxy-tab-s10-ultra", "https://images.unsplash.com/photo-1561154464-82e9adf32764", "Galaxy Tab S10 Ultra display");
        attachIfEmpty("keychron-q1-pro", "https://images.unsplash.com/photo-1587829741301-dc798b83add3", "Keychron Q1 Pro mechanical keyboard");
        attachIfEmpty("anker-prime-magsafe", "https://images.unsplash.com/photo-1583863788434-e58a36330cf0", "Anker Prime MagSafe Charging Station");
        attachIfEmpty("anker-prime-charging-stand", "https://images.unsplash.com/photo-1583863788434-e58a36330cf0", "Anker Prime Wireless Charging Stand");
        attachIfEmpty("dyson-solarcycle-morph", "https://images.unsplash.com/photo-1507473885765-e6ed057f782c", "Dyson Solarcycle Morph task light");
    }

    private void attachIfEmpty(String slug, String url, String altText) {
        productRepository.findBySlug(slug).ifPresent(p -> {
            if (p.getImages().isEmpty()) {
                p.addImage(url, altText, 1);
                productRepository.save(p);
                log.info("Attached image to existing product: {}", slug);
            }
        });
    }

    private Brand brand(String name, String slug) {
        return brandRepository.findBySlug(slug).orElseGet(() -> brandRepository.save(new Brand(name, slug)));
    }

    private Category category(String name, String slug) {
        return categoryRepository.findBySlug(slug).orElseGet(() -> categoryRepository.save(new Category(name, slug)));
    }

    // --- PHONES ---

    private void seedIPhone16ProMax(Brand brand, Category category) {
        if (productRepository.existsBySlug("iphone-16-pro-max")) return;
        Product p = new Product("iPhone 16 Pro Max", "iphone-16-pro-max",
                "Titanium excellence with A18 Pro chip, 48MP Fusion camera, and Camera Control.",
                "Crafted in Grade 5 titanium with an expansive 6.9-inch Super Retina XDR display, ProMotion technology, and next-generation battery longevity.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1695048133142-1a20484d2569", "iPhone 16 Pro Max Desert Titanium", 1);
        p.addImage("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5", "iPhone camera lens detail", 2);
        p.addSpecification("Display", "6.9 inch Super Retina XDR OLED, 120Hz ProMotion", 1);
        p.addSpecification("Processor", "Apple A18 Pro chip with 6-core GPU", 2);
        p.addSpecification("Camera", "48MP Fusion + 48MP Ultra Wide + 12MP 5x Telephoto", 3);
        p.addSpecification("Material", "Grade 5 Titanium with textured matte glass back", 4);
        p.addVariant(new ProductVariant("APL-16PM-DTI-256", "Desert Titanium", "256GB", new BigDecimal("1199.00"), 25));
        p.addVariant(new ProductVariant("APL-16PM-NTI-512", "Natural Titanium", "512GB", new BigDecimal("1399.00"), 15));
        p.addVariant(new ProductVariant("APL-16PM-BTI-1TB", "Black Titanium", "1TB", new BigDecimal("1599.00"), 8));
        productRepository.save(p);
    }

    private void seedGalaxyS25Ultra(Brand brand, Category category) {
        if (productRepository.existsBySlug("galaxy-s25-ultra")) return;
        Product p = new Product("Galaxy S25 Ultra", "galaxy-s25-ultra",
                "Galaxy AI powerhouse with built-in S Pen, 200MP camera, and Snapdragon 8 Elite.",
                "Redesigned rounded titanium corners for superior comfort, anti-reflective flat display, and real-time on-device multilingual translation.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf", "Galaxy S25 Ultra titanium frame", 1);
        p.addImage("https://images.unsplash.com/photo-1580910051074-3eb694886505", "Galaxy S25 Ultra display view", 2);
        p.addSpecification("Display", "6.8 inch Dynamic AMOLED 2X, 1-120Hz, 2600 nits", 1);
        p.addSpecification("Processor", "Qualcomm Snapdragon 8 Elite for Galaxy", 2);
        p.addSpecification("Camera", "200MP Wide + 50MP Periscope Telephoto + 50MP Ultra Wide", 3);
        p.addSpecification("Stylus", "Integrated Bluetooth S Pen", 4);
        p.addVariant(new ProductVariant("SAM-S25U-TGR-256", "Titanium Gray", "256GB", new BigDecimal("1299.00"), 20));
        p.addVariant(new ProductVariant("SAM-S25U-TBK-512", "Titanium Black", "512GB", new BigDecimal("1419.00"), 12));
        productRepository.save(p);
    }

    private void seedPixel9Pro(Brand brand, Category category) {
        if (productRepository.existsBySlug("pixel-9-pro")) return;
        Product p = new Product("Pixel 9 Pro", "pixel-9-pro",
                "Google Tensor G4, pro-level triple cameras, and Gemini Live AI assistant.",
                "Sleek matte glass back with polished dual-finish camera bar, 7 years of OS updates, and Super Actua display.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1598327105666-5b89351aff97", "Pixel 9 Pro in Obsidian", 1);
        p.addSpecification("Display", "6.3 inch Super Actua OLED 1-120Hz", 1);
        p.addSpecification("Processor", "Google Tensor G4 with Titan M2 security", 2);
        p.addSpecification("Camera", "50MP wide + 48MP telephoto (5x optical zoom)", 3);
        p.addVariant(new ProductVariant("GOO-P9P-OBS-128", "Obsidian", "128GB", new BigDecimal("999.00"), 16));
        p.addVariant(new ProductVariant("GOO-P9P-POR-256", "Porcelain", "256GB", new BigDecimal("1099.00"), 10));
        productRepository.save(p);
    }

    // --- LAPTOPS ---

    private void seedMacBookPro16(Brand brand, Category category) {
        if (productRepository.existsBySlug("macbook-pro-16-m4")) return;
        Product p = new Product("MacBook Pro 16 M4 Max", "macbook-pro-16-m4",
                "Extreme pro performance with M4 Max, Liquid Retina XDR display, and 24-hour battery.",
                "Engineered for demanding creative and engineering workflows. Features up to 16-core CPU, 40-core GPU, and Liquid Retina XDR with nano-texture glass option.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1517336714731-489689fd1ca8", "MacBook Pro 16 Space Black", 1);
        p.addImage("https://images.unsplash.com/photo-1611186871348-b1ce696e52c9", "MacBook Pro keyboard and screen", 2);
        p.addSpecification("Display", "16.2 inch Liquid Retina XDR, 1600 nits peak, 120Hz", 1);
        p.addSpecification("Processor", "Apple M4 Max with 16-core CPU, 40-core GPU", 2);
        p.addSpecification("Battery", "100Wh lithium-polymer with 140W USB-C fast charging", 3);
        p.addVariant(new ProductVariant("APL-MBP16-SBK-1TB", "Space Black", "36GB / 1TB SSD", new BigDecimal("2499.00"), 10));
        p.addVariant(new ProductVariant("APL-MBP16-SIL-2TB", "Silver", "48GB / 2TB SSD", new BigDecimal("3499.00"), 6));
        productRepository.save(p);
    }

    private void seedDellXps15(Brand brand, Category category) {
        if (productRepository.existsBySlug("dell-xps-15-oled")) return;
        Product p = new Product("Dell XPS 15 OLED Touch", "dell-xps-15-oled",
                "Minimalist aluminum chassis, 3.5K OLED InfinityEdge touchscreen, and Intel Core Ultra 9.",
                "Combining carbon-fiber palm rest with CNC machined aluminum. Tuned for photo and video editors who need color-accurate 100% DCI-P3 visuals.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1593642632823-8f785ba67e45", "Dell XPS 15 display open", 1);
        p.addSpecification("Display", "15.6 inch 3.5K (3456x2160) OLED Touchscreen", 1);
        p.addSpecification("Processor", "Intel Core Ultra 9 185H with dedicated AI NPU", 2);
        p.addSpecification("Graphics", "NVIDIA GeForce RTX 4070 8GB GDDR6", 3);
        p.addVariant(new ProductVariant("DEL-XPS15-SLV-512", "Platinum Silver", "16GB / 512GB SSD", new BigDecimal("1899.00"), 12));
        p.addVariant(new ProductVariant("DEL-XPS15-SLV-1TB", "Platinum Silver", "32GB / 1TB SSD", new BigDecimal("2299.00"), 7));
        productRepository.save(p);
    }

    private void seedThinkPadX1(Brand brand, Category category) {
        if (productRepository.existsBySlug("thinkpad-x1-carbon-gen12")) return;
        Product p = new Product("ThinkPad X1 Carbon Gen 12", "thinkpad-x1-carbon-gen12",
                "Ultralight business flagship with carbon fiber lid, ergonomic keyboard, and LTE option.",
                "Weighing under 1.1kg, the X1 Carbon offers enterprise MIL-STD-810H durability, discrete TPM 2.0 security, and legendary keyboard travel.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed", "ThinkPad X1 Carbon matte black", 1);
        p.addSpecification("Weight", "1.09 kg ultra-portable carbon fiber", 1);
        p.addSpecification("Display", "14 inch 2.8K OLED 120Hz anti-glare", 2);
        p.addSpecification("Security", "Fingerprint reader + IR camera with presence detection", 3);
        p.addVariant(new ProductVariant("THK-X1C-BLK-512", "Deep Black", "16GB / 512GB SSD", new BigDecimal("1699.00"), 14));
        p.addVariant(new ProductVariant("THK-X1C-BLK-1TB", "Deep Black", "32GB / 1TB SSD", new BigDecimal("1999.00"), 8));
        productRepository.save(p);
    }

    private void seedZephyrusG16(Brand brand, Category category) {
        if (productRepository.existsBySlug("rog-zephyrus-g16")) return;
        Product p = new Product("ROG Zephyrus G16 OLED", "rog-zephyrus-g16",
                "Ultra-slim gaming and creator notebook with 240Hz ROG Nebula OLED and Slash Lighting.",
                "Precision CNC-machined unibody with vapor chamber cooling, quad-speaker Dolby Atmos system, and high-performance RTX 4080.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1603302576837-37561b2e2302", "Zephyrus G16 open on desk", 1);
        p.addSpecification("Display", "16 inch 2.5K 240Hz 0.2ms OLED ROG Nebula Display", 1);
        p.addSpecification("Processor", "AMD Ryzen 9 AI 370 HX processor", 2);
        p.addSpecification("Graphics", "NVIDIA GeForce RTX 4080 Laptop GPU", 3);
        p.addVariant(new ProductVariant("ROG-G16-GRY-1TB", "Eclipse Gray", "32GB / 1TB SSD", new BigDecimal("1999.00"), 9));
        p.addVariant(new ProductVariant("ROG-G16-WHT-2TB", "Platinum White", "32GB / 2TB SSD", new BigDecimal("2499.00"), 5));
        productRepository.save(p);
    }

    // --- AUDIO ---

    private void seedSonyWh1000Xm5(Brand brand, Category category) {
        if (productRepository.existsBySlug("sony-wh-1000xm5")) return;
        Product p = new Product("Sony WH-1000XM5", "sony-wh-1000xm5",
                "Industry-leading noise canceling headphones with dual processors and 8 microphones.",
                "Auto NC Optimizer adjusts noise canceling automatically based on wearing conditions and environment. Specially engineered 30mm driver unit with carbon fiber composite.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1546435770-a3e426bf472b", "Sony WH-1000XM5 headphones", 1);
        p.addSpecification("Noise Canceling", "Dual processor V1 + QN1 noise cancelling chip", 1);
        p.addSpecification("Battery", "30 hours battery life with quick charging (3 min for 3 hours)", 2);
        p.addSpecification("Codecs", "LDAC, AAC, SBC with Hi-Res Audio Wireless certification", 3);
        p.addVariant(new ProductVariant("SNY-XM5-BLK", "Midnight Black", "Standard", new BigDecimal("399.00"), 30));
        p.addVariant(new ProductVariant("SNY-XM5-SLV", "Silver Sand", "Standard", new BigDecimal("399.00"), 18));
        productRepository.save(p);
    }

    private void seedAirPodsMax(Brand brand, Category category) {
        if (productRepository.existsBySlug("airpods-max")) return;
        Product p = new Product("AirPods Max USB-C", "airpods-max",
                "High-fidelity audio with Personalised Spatial Audio and USB-C charging.",
                "Anodized aluminum earcups with breathable knit mesh canopy, Apple-designed dynamic driver, and computational audio with dual H1 chips.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1583394838336-acd977736f90", "AirPods Max premium headphones", 1);
        p.addSpecification("Acoustics", "Apple-designed dynamic driver with dual neodymium ring magnet", 1);
        p.addSpecification("Charging", "USB-C universal charging with Smart Case included", 2);
        p.addSpecification("Audio Features", "Personalised Spatial Audio with dynamic head tracking", 3);
        p.addVariant(new ProductVariant("APL-APM-MDN", "Midnight", "USB-C", new BigDecimal("549.00"), 16));
        p.addVariant(new ProductVariant("APL-APM-STL", "Starlight", "USB-C", new BigDecimal("549.00"), 12));
        p.addVariant(new ProductVariant("APL-APM-SPG", "Space Gray", "USB-C", new BigDecimal("549.00"), 8));
        productRepository.save(p);
    }

    private void seedBoseQcUltra(Brand brand, Category category) {
        if (productRepository.existsBySlug("bose-qc-ultra")) return;
        Product p = new Product("Bose QuietComfort Ultra", "bose-qc-ultra",
                "World-class noise cancellation, breakthrough spatialized audio, and ultra-luxe comfort.",
                "CustomTune technology personalizes sound performance to your ears shape. Immersive Audio places music directly in front of you.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1572536147248-ac59a8abfa4b", "Bose QC Ultra headphones", 1);
        p.addSpecification("Spatial Sound", "Bose Immersive Audio mode", 1);
        p.addSpecification("Materials", "Protein leather cushions with cast aluminum yokes", 2);
        p.addSpecification("Battery", "Up to 24 hours (18 hours with Immersive Audio)", 3);
        p.addVariant(new ProductVariant("BOS-QCU-BLK", "Black", "Standard", new BigDecimal("429.00"), 20));
        p.addVariant(new ProductVariant("BOS-QCU-WHT", "White Smoke", "Standard", new BigDecimal("429.00"), 15));
        productRepository.save(p);
    }

    private void seedDevialetMania(Brand brand, Category category) {
        if (productRepository.existsBySlug("devialet-mania")) return;
        Product p = new Product("Devialet Mania Opera Gold", "devialet-mania",
                "Luxurious high-fidelity portable speaker with 360-degree stereo and 24K gold plating.",
                "Devialet active stereo calibration technology adapts rendering to room acoustics in real time. Features 4 full-range drivers and 2 subwoofers.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1545454675-3531b543be5d", "Devialet portable high-end speaker", 1);
        p.addSpecification("Power", "176 Watts total amplification", 1);
        p.addSpecification("Frequency Response", "30Hz - 20kHz acoustic coverage", 2);
        p.addSpecification("Protection", "IPX4 splash resistance with 10 hours battery", 3);
        p.addVariant(new ProductVariant("DEV-MAN-GLD", "Opera de Paris (24K Gold)", "Special Edition", new BigDecimal("990.00"), 6));
        p.addVariant(new ProductVariant("DEV-MAN-GRY", "Light Grey", "Standard", new BigDecimal("790.00"), 12));
        productRepository.save(p);
    }

    // --- WEARABLES ---

    private void seedAppleWatchUltra2(Brand brand, Category category) {
        if (productRepository.existsBySlug("apple-watch-ultra-2")) return;
        Product p = new Product("Apple Watch Ultra 2", "apple-watch-ultra-2",
                "Rugged titanium sports watch with precision dual-frequency GPS, S9 SiP, and 3000 nits display.",
                "Designed for endurance athletes, outdoor adventurers, and water sports enthusiasts with 100m water resistance and EN13319 dive certification.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1", "Apple Watch Ultra 2 titanium", 1);
        p.addSpecification("Case", "49mm aerospace-grade titanium case with flat sapphire crystal", 1);
        p.addSpecification("Water Resistance", "100m water resistant, certified for scuba diving to 40m", 2);
        p.addSpecification("Battery", "Up to 36 hours normal use (72 hours in Low Power Mode)", 3);
        p.addVariant(new ProductVariant("APL-AWU2-ALP", "Titanium Case", "Alpine Loop Blue", new BigDecimal("799.00"), 15));
        p.addVariant(new ProductVariant("APL-AWU2-TRL", "Titanium Case", "Trail Loop Orange", new BigDecimal("799.00"), 12));
        productRepository.save(p);
    }

    private void seedGalaxyWatchUltra(Brand brand, Category category) {
        if (productRepository.existsBySlug("galaxy-watch-ultra")) return;
        Product p = new Product("Galaxy Watch Ultra 47mm", "galaxy-watch-ultra",
                "Titanium cushion design, dual-frequency GPS, Multi-sports tile, and Energy Score AI.",
                "Built to withstand extreme environments with 10ATM water resistance, sapphire glass, and customizable Quick Button for quick workouts.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1523275335684-37898b6baf30", "Galaxy Watch Ultra", 1);
        p.addSpecification("Case", "47mm Grade 4 Titanium cushion frame", 1);
        p.addSpecification("Display", "1.5 inch Super AMOLED 480x480, 3000 nits", 2);
        p.addSpecification("Health", "BioActive Sensor (ECG, Blood Pressure, BIA analysis)", 3);
        p.addVariant(new ProductVariant("SAM-GWU-TGY", "Titanium Gray", "Dark Gray Marine Band", new BigDecimal("649.00"), 16));
        p.addVariant(new ProductVariant("SAM-GWU-TSL", "Titanium Silver", "Orange Marine Band", new BigDecimal("649.00"), 10));
        productRepository.save(p);
    }

    private void seedGarminFenix8(Brand brand, Category category) {
        if (productRepository.existsBySlug("garmin-fenix-8")) return;
        Product p = new Product("Garmin Fenix 8 Sapphire", "garmin-fenix-8",
                "Multisport GPS smartwatch with bright AMOLED display, titanium bezel, and built-in flashlight.",
                "Engineered for athletes with advanced strength training plans, leakproof sensor buttons, and up to 29 days battery in smartwatch mode.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1579586337278-3befd40fd17a", "Garmin Fenix smartwatch", 1);
        p.addSpecification("Display", "1.4 inch bright AMOLED touchscreen with Sapphire crystal", 1);
        p.addSpecification("Bezel", "DLC titanium bezel with reinforced polymer case", 2);
        p.addSpecification("Sensors", "Elevate Gen 5 heart rate sensor with ECG app support", 3);
        p.addVariant(new ProductVariant("GAR-F8-47MM", "Carbon Gray Titanium", "47mm AMOLED", new BigDecimal("999.00"), 10));
        p.addVariant(new ProductVariant("GAR-F8-51MM", "Carbon Gray Titanium", "51mm AMOLED", new BigDecimal("1099.00"), 7));
        productRepository.save(p);
    }

    // --- TABLETS ---

    private void seedIPadPro13(Brand brand, Category category) {
        if (productRepository.existsBySlug("ipad-pro-13-m4")) return;
        Product p = new Product("iPad Pro 13 M4 Ultra-thin", "ipad-pro-13-m4",
                "Impossibly thin 5.1mm design with Ultra Retina XDR tandem OLED display and M4 power.",
                "Breakthrough tandem OLED technology delivers 1000 nits full-screen brightness, ProRes video capture, and Apple Pencil Pro barrel roll support.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1561154464-82e9adf32764", "iPad Pro 13 front and pen", 1);
        p.addSpecification("Thickness", "5.1 mm ultra-thin aluminum unibody", 1);
        p.addSpecification("Display", "13.0 inch Ultra Retina XDR Tandem OLED, 1600 nits peak", 2);
        p.addSpecification("Processor", "Apple M4 chip with Hardware-accelerated ray tracing", 3);
        p.addVariant(new ProductVariant("APL-IPP13-SBK-256", "Space Black", "256GB Wi-Fi", new BigDecimal("1299.00"), 18));
        p.addVariant(new ProductVariant("APL-IPP13-SIL-512", "Silver", "512GB Wi-Fi", new BigDecimal("1499.00"), 12));
        p.addVariant(new ProductVariant("APL-IPP13-SBK-1TB", "Space Black", "1TB Nano-texture Glass", new BigDecimal("1899.00"), 5));
        productRepository.save(p);
    }

    private void seedGalaxyTabS10(Brand brand, Category category) {
        if (productRepository.existsBySlug("galaxy-tab-s10-ultra")) return;
        Product p = new Product("Galaxy Tab S10 Ultra", "galaxy-tab-s10-ultra",
                "Gigantic 14.6 inch Dynamic AMOLED 2X with anti-reflection coating, S Pen included, and IP68 rating.",
                "Engineered for serious multi-window productivity with Samsung DeX, Armor Aluminum frame, and AI Drawing Assist.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1589739900243-4b52cd9b104e", "Galaxy Tab S10 Ultra widescreen", 1);
        p.addSpecification("Display", "14.6 inch Dynamic AMOLED 2X, 120Hz, Anti-reflection coating", 1);
        p.addSpecification("Durability", "Enhanced Armor Aluminum with IP68 water & dust resistance", 2);
        p.addSpecification("Included Accessories", "Low-latency IP68 S Pen in the box", 3);
        p.addVariant(new ProductVariant("SAM-TS10-GRY-256", "Moonstone Gray", "256GB Wi-Fi", new BigDecimal("1199.00"), 14));
        p.addVariant(new ProductVariant("SAM-TS10-SLV-512", "Platinum Silver", "512GB Wi-Fi", new BigDecimal("1319.00"), 8));
        productRepository.save(p);
    }

    // --- ACCESSORIES ---

    private void seedKeychronQ1Pro(Brand brand, Category category) {
        if (productRepository.existsBySlug("keychron-q1-pro")) return;
        Product p = new Product("Keychron Q1 Pro Wireless Custom", "keychron-q1-pro",
                "Full-metal QMK/VIA wireless mechanical keyboard with double-gasket design and rotary knob.",
                "Crafted with 6063 CNC aluminum body, hot-swappable Keychron K Pro switches, South-facing RGB, and screw-in PCB stabilizers.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1587829741301-dc798b83add3", "Keychron custom mechanical keyboard", 1);
        p.addSpecification("Body Material", "Full CNC machined 6063 aluminum body", 1);
        p.addSpecification("Connectivity", "Bluetooth 5.1 + Type-C wired mode", 2);
        p.addSpecification("Switches", "Hot-swappable Keychron K Pro pre-lubed switches", 3);
        p.addVariant(new ProductVariant("KEY-Q1P-RED", "Carbon Black", "K Pro Red (Linear)", new BigDecimal("199.00"), 22));
        p.addVariant(new ProductVariant("KEY-Q1P-BAN", "Shell White", "K Pro Banana (Tactile)", new BigDecimal("219.00"), 15));
        productRepository.save(p);
    }

    private void seedAnkerPrimeStand(Brand brand, Category category) {
        if (productRepository.existsBySlug("anker-prime-magsafe")) return;
        Product p = new Product("Anker Prime MagSafe Charging Station", "anker-prime-magsafe",
                "3-in-1 15W Qi2 fast wireless charging tree with aerospace-grade anodized aluminum finish.",
                "Simultaneously fast charges iPhone, Apple Watch, and AirPods with adjustable 70-degree viewing angle and ActiveShield 2.0 temperature protection.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1622445262464-84b1456045b6", "Anker MagSafe charging stand", 1);
        p.addSpecification("Charging Tech", "Official Qi2 15W certified fast wireless charging", 1);
        p.addSpecification("Compatibility", "iPhone 12-16 series, Apple Watch Series 1-10/Ultra, AirPods Pro", 2);
        p.addSpecification("Safety", "ActiveShield 2.0 monitors temperature 3,000,000 times per day", 3);
        p.addVariant(new ProductVariant("ANK-PRM-MBK", "Anodized Black", "15W 3-in-1", new BigDecimal("129.00"), 28));
        p.addVariant(new ProductVariant("ANK-PRM-SLV", "Silver Frost", "15W 3-in-1", new BigDecimal("129.00"), 20));
        productRepository.save(p);
    }

    private void seedDysonSolarcycle(Brand brand, Category category) {
        if (productRepository.existsBySlug("dyson-solarcycle-morph")) return;
        Product p = new Product("Dyson Solarcycle Morph Desk Light", "dyson-solarcycle-morph",
                "Intelligent tracking desk light that transforms 4 ways and adapts to local daylight.",
                "Heat pipe cooling technology preserves light quality for 60 years. Features ambient, task, feature, and indirect lighting modes via optical head rotation.",
                brand, category);
        p.addImage("https://images.unsplash.com/photo-1507473885765-e6ed057f782c", "Dyson luxury modern desk light", 1);
        p.addSpecification("Light Quality", "Glare-free illumination with color rendering index of 90+", 1);
        p.addSpecification("Smart Features", "Daylight tracking algorithm with motion sensor auto-off", 2);
        p.addSpecification("Connectivity", "Dyson Link app customization for age and activity", 3);
        p.addVariant(new ProductVariant("DYS-SCM-MBK", "Matte Black", "Desk Mount", new BigDecimal("649.00"), 12));
        p.addVariant(new ProductVariant("DYS-SCM-BRS", "Brass Finish", "Desk Mount", new BigDecimal("699.00"), 8));
        productRepository.save(p);
    }

    private void seedBanners() {
        if (bannerRepository.count() > 0) return;
        Banner b1 = new Banner(
                "Haute Horlogerie Grand Exhibition",
                "Timeless Precision & Hand-Finished Complications from Heritage Maisons",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1600&auto=format&fit=crop&q=80",
                "/products",
                "Explore Masterpieces",
                1,
                true,
                null,
                null
        );
        Banner b2 = new Banner(
                "Celestial Diamond & High Jewelry",
                "Exquisite Craftsmanship in Platinum & 18K Rose Gold with Certified Gems",
                "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=1600&auto=format&fit=crop&q=80",
                "/products",
                "Discover High Jewelry",
                2,
                true,
                null,
                null
        );
        Banner b3 = new Banner(
                "Private Salon & Bespoke Concierge",
                "Privilege Access for Connoisseurs, White-Glove Logistics & Loyalty Ascension",
                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=1600&auto=format&fit=crop&q=80",
                "/account",
                "Ascend to Privilege",
                3,
                true,
                null,
                null
        );
        bannerRepository.saveAll(java.util.List.of(b1, b2, b3));
        log.info("Seeded 3 luxury showcase banners.");
    }
}
