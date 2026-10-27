import type { ProductDetail, ProductListItem } from "./types";

export const FALLBACK_PRODUCTS: ProductListItem[] = [
  {
    id: "fb-1",
    name: "AeroPhone 16 Pro Max",
    slug: "aerophone-16-pro-max",
    subtitle: "Khung Titanium hàng không vũ trụ · A18 Pro · Nút điều khiển camera chuyên nghiệp",
    brand: { id: "b-1", name: "Aero", slug: "aero" },
    category: { id: "c-1", name: "Smartphones", slug: "smartphones" },
    minPrice: "1499.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-2",
    name: "Chrono Horizon Master",
    slug: "chrono-horizon-master",
    subtitle: "Kính Sapphire nguyên khối · Vỏ gốm Ceramic đen bóng · Tourbillon kỹ thuật số",
    brand: { id: "b-2", name: "Chrono", slug: "chrono" },
    category: { id: "c-2", name: "Smart Horology", slug: "horology" },
    minPrice: "2850.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-3",
    name: "Opus Noir Studio Sound System",
    slug: "opus-noir-sound-system",
    subtitle: "Hệ thống âm thanh Hi-Res đa kênh mạ vàng 24K · Màng loa Beryllium đỉnh cao",
    brand: { id: "b-3", name: "Opus", slug: "opus" },
    category: { id: "c-3", name: "Audio & Acoustics", slug: "audio" },
    minPrice: "3400.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-4",
    name: "Lumina Carbon Wireless Earbuds",
    slug: "lumina-carbon-earbuds",
    subtitle: "Chống ồn chủ động thích ứng · Hộp sạc sợi Carbon rèn · Thời lượng pin 48 giờ",
    brand: { id: "b-4", name: "Lumina", slug: "lumina" },
    category: { id: "c-3", name: "Audio & Acoustics", slug: "audio" },
    minPrice: "590.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-5",
    name: "AeroBook Ultra Studio M4 Max",
    slug: "aerobook-ultra-studio",
    subtitle: "Màn hình Liquid Retina XDR Nano-texture · RAM 64GB thống nhất · Khung nhôm đen tuyền",
    brand: { id: "b-1", name: "Aero", slug: "aero" },
    category: { id: "c-1", name: "Smartphones", slug: "smartphones" },
    minPrice: "3899.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-6",
    name: "Chrono Stellar Celestial Watch",
    slug: "chrono-stellar-celestial",
    subtitle: "Dây da cá sấu Alligator Pháp · Mặt số thiên thạch tự nhiên · Dự trữ cót 72 giờ",
    brand: { id: "b-2", name: "Chrono", slug: "chrono" },
    category: { id: "c-2", name: "Smart Horology", slug: "horology" },
    minPrice: "4200.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-7",
    name: "LUXORA Maison Leather Folio",
    slug: "luxora-maison-folio",
    subtitle: "Da Bê Epsom Pháp khâu tay thủ công · Khóa Titan mạ vàng khắc laser Maison",
    brand: { id: "b-5", name: "LUXORA Studio", slug: "luxora-studio" },
    category: { id: "c-4", name: "Luxury Accessories", slug: "accessories" },
    minPrice: "320.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-8",
    name: "AeroPad Pro OLED Ultra",
    slug: "aeropad-pro-oled",
    subtitle: "Tấm nền Tandem OLED kép siêu mỏng 5.1mm · Chip M4 thế hệ mới · Vỏ titan sa mạc",
    brand: { id: "b-1", name: "Aero", slug: "aero" },
    category: { id: "c-1", name: "Smartphones", slug: "smartphones" },
    minPrice: "1299.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=1000&auto=format&fit=crop&q=80"
  },
  {
    id: "fb-9",
    name: "Opus Clarity Pure Beryllium",
    slug: "opus-clarity-pure",
    subtitle: "Tai nghe Over-ear mở mạ Rhodium · Đệm da cừu non Lambskin êm ái tuyệt đối",
    brand: { id: "b-3", name: "Opus", slug: "opus" },
    category: { id: "c-3", name: "Audio & Acoustics", slug: "audio" },
    minPrice: "1850.00",
    inStock: true,
    imageUrl: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=1000&auto=format&fit=crop&q=80"
  }
];

export const FALLBACK_DETAILS: Record<string, ProductDetail> = {
  "aerophone-16-pro-max": {
    id: "fb-1",
    name: "AeroPhone 16 Pro Max",
    slug: "aerophone-16-pro-max",
    subtitle: "Khung Titanium hàng không vũ trụ · A18 Pro · Nút điều khiển camera chuyên nghiệp",
    description: "Được tôi luyện từ titan Cấp 5 siêu bền và nhẹ, AeroPhone 16 Pro Max là biểu tượng của công nghệ và sự sang trọng. Viền màn hình mỏng nhất từng có, mặt kính Ceramic Shield thế hệ mới nhất và chip xử lý A18 Pro mang đến hiệu năng đồ họa đẳng cấp phòng thu.",
    brand: { id: "b-1", name: "Aero", slug: "aero" },
    category: { id: "c-1", name: "Smartphones", slug: "smartphones" },
    images: [
      { id: "img-1-1", url: "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=1200&auto=format&fit=crop&q=80", altText: "AeroPhone 16 Pro Max Desert Titanium", displayOrder: 1 },
      { id: "img-1-2", url: "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=1200&auto=format&fit=crop&q=80", altText: "Camera control sapphire lens", displayOrder: 2 }
    ],
    specifications: [
      { id: "s-1", name: "Chất liệu khung", value: "Titan Cấp 5 viền phay xước tinh xảo", displayOrder: 1 },
      { id: "s-2", name: "Màn hình", value: "Super Retina XDR 6.9 inch, ProMotion 120Hz", displayOrder: 2 },
      { id: "s-3", name: "Vi xử lý", value: "A18 Pro 3nm thế hệ thứ 2, 6-core GPU", displayOrder: 3 },
      { id: "s-4", name: "Camera chính", value: "48MP Fusion, Telephoto 5x lăng kính tứ giác", displayOrder: 4 },
      { id: "s-5", name: "Bảo hành Maison", value: "2 năm đặc quyền LUXORA Concierge VIP", displayOrder: 5 }
    ],
    variants: [
      { id: "v-1-1", sku: "AERO-16-NAT-256", color: "Titan Tự Nhiên", storage: "256GB", price: "1499.00", inStock: true },
      { id: "v-1-2", sku: "AERO-16-NAT-512", color: "Titan Tự Nhiên", storage: "512GB", price: "1799.00", inStock: true },
      { id: "v-1-3", sku: "AERO-16-DES-1TB", color: "Titan Sa Mạc Gold", storage: "1TB", price: "2199.00", inStock: true }
    ]
  },
  "chrono-horizon-master": {
    id: "fb-2",
    name: "Chrono Horizon Master",
    slug: "chrono-horizon-master",
    subtitle: "Kính Sapphire nguyên khối · Vỏ gốm Ceramic đen bóng · Tourbillon kỹ thuật số",
    description: "Chrono Horizon Master kết hợp tinh hoa chế tác đồng hồ Thụy Sĩ với vi điện tử hàng đầu. Vỏ bằng gốm đen nung 1,500 độ C không thể trầy xước, viền bezel vát kim cương và cơ chế hiển thị thời gian tourbillon tương tác độc bản.",
    brand: { id: "b-2", name: "Chrono", slug: "chrono" },
    category: { id: "c-2", name: "Smart Horology", slug: "horology" },
    images: [
      { id: "img-2-1", url: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1200&auto=format&fit=crop&q=80", altText: "Chrono Horizon Master Ceramic", displayOrder: 1 },
      { id: "img-2-2", url: "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=1200&auto=format&fit=crop&q=80", altText: "Chrono Bezel and Dial", displayOrder: 2 }
    ],
    specifications: [
      { id: "s-1", name: "Chất liệu vỏ", value: "High-tech Ceramic đen huyền bí chống xước", displayOrder: 1 },
      { id: "s-2", name: "Mặt kính", value: "Sapphire Crystal cong vòm phủ chống lóa kép", displayOrder: 2 },
      { id: "s-3", name: "Độ chống nước", value: "10 ATM (100 mét) chuẩn bơi lặn Maison", displayOrder: 3 },
      { id: "s-4", name: "Dây đeo", value: "Da bê Barenia Pháp & Cao su FKM cao cấp", displayOrder: 4 },
      { id: "s-5", name: "Bảo hành", value: "Bảo hành toàn cầu 5 năm chính hãng Chrono", displayOrder: 5 }
    ],
    variants: [
      { id: "v-2-1", sku: "CHRONO-HM-BLK-44", color: "Noir Ceramic", storage: "44mm", price: "2850.00", inStock: true },
      { id: "v-2-2", sku: "CHRONO-HM-GLD-44", color: "Champagne Gold Bezel", storage: "44mm", price: "3250.00", inStock: true }
    ]
  },
  "opus-noir-sound-system": {
    id: "fb-3",
    name: "Opus Noir Studio Sound System",
    slug: "opus-noir-sound-system",
    subtitle: "Hệ thống âm thanh Hi-Res đa kênh mạ vàng 24K · Màng loa Beryllium đỉnh cao",
    description: "Âm thanh nguyên bản đúng như ý đồ của nhà soạn nhạc tại phòng thu Abbey Road. Hệ thống Opus Noir sử dụng thùng loa composite đúc nguyên khối chống cộng hưởng, củ loa màng Beryllium siêu nhẹ và mạch khuếch đại thuần Class-A mạ vàng.",
    brand: { id: "b-3", name: "Opus", slug: "opus" },
    category: { id: "c-3", name: "Audio & Acoustics", slug: "audio" },
    images: [
      { id: "img-3-1", url: "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=1200&auto=format&fit=crop&q=80", altText: "Opus Noir Studio Sound System", displayOrder: 1 }
    ],
    specifications: [
      { id: "s-1", name: "Cấu hình loa", value: "Hệ thống 3 đường tiếng Active Hi-End", displayOrder: 1 },
      { id: "s-2", name: "Màng loa cao tần", value: "Beryllium 25mm nguyên chất phủ hơi vàng", displayOrder: 2 },
      { id: "s-3", name: "Công suất", value: "850W RMS Class-A/AB Hybrid", displayOrder: 3 },
      { id: "s-4", name: "Kết nối", value: "Wi-Fi 7, AirPlay 2, Roon Ready, XLR Balanced", displayOrder: 4 }
    ],
    variants: [
      { id: "v-3-1", sku: "OPUS-NOIR-PAIR", color: "Obsidian Black & Gold", storage: "Cặp Loa Studio", price: "3400.00", inStock: true }
    ]
  }
};

export function getFallbackProductDetail(slug: string): ProductDetail {
  if (FALLBACK_DETAILS[slug]) {
    return FALLBACK_DETAILS[slug];
  }

  const item = FALLBACK_PRODUCTS.find((p) => p.slug === slug);
  if (item) {
    return {
      id: item.id,
      name: item.name,
      slug: item.slug,
      subtitle: item.subtitle,
      description: `${item.name} là tác phẩm tiêu biểu của Maison LUXORA, được chế tác từ những vật liệu tinh hoa nhất với độ hoàn thiện hoàn mỹ. Sở hữu công nghệ tân tiến nhất thế giới trong một thiết kế đậm chất nghệ thuật.`,
      brand: item.brand,
      category: item.category,
      images: item.imageUrl ? [{ id: `img-${item.id}-1`, url: item.imageUrl, altText: item.name, displayOrder: 1 }] : [],
      specifications: [
        { id: "s-1", name: "Thương hiệu chế tác", value: item.brand.name, displayOrder: 1 },
        { id: "s-2", name: "Phân hạng Maison", value: "VIP Haute Technologie", displayOrder: 2 },
        { id: "s-3", name: "Nguồn gốc xuất xứ", value: "Chính hãng nhập khẩu Châu Âu", displayOrder: 3 },
        { id: "s-4", name: "Đặc quyền bảo dưỡng", value: "LUXORA White-Glove VIP Concierge", displayOrder: 4 }
      ],
      variants: [
        { id: `v-${item.id}-1`, sku: `${item.slug.toUpperCase()}-STD`, color: "Signature Gold Noir", storage: "Standard Edition", price: item.minPrice ?? "999.00", inStock: true },
        { id: `v-${item.id}-2`, sku: `${item.slug.toUpperCase()}-VIP`, color: "Titanium Champagne", storage: "Collector VIP Edition", price: String(Number(item.minPrice ?? 999) + 350) + ".00", inStock: true }
      ]
    };
  }

  return FALLBACK_DETAILS["aerophone-16-pro-max"];
}

export const DEFAULT_PRODUCT_IMAGES: Record<string, string> = {
  "aerophone-x1": "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop&q=80",
  "studio-one": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80",
  "vision-slate": "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop&q=80",
  "iphone-16-pro-max": "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&auto=format&fit=crop&q=80",
  "galaxy-s25-ultra": "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&auto=format&fit=crop&q=80",
  "pixel-9-pro": "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&auto=format&fit=crop&q=80",
  "macbook-pro-16-m4": "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop&q=80",
  "dell-xps-15-oled": "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=800&auto=format&fit=crop&q=80",
  "thinkpad-x1-carbon-gen12": "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80",
  "rog-zephyrus-g16": "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&auto=format&fit=crop&q=80",
  "sony-wh-1000xm5": "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80",
  "airpods-max": "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=800&auto=format&fit=crop&q=80",
  "bose-qc-ultra": "https://images.unsplash.com/photo-1572536147248-ac59a8abfa4b?w=800&auto=format&fit=crop&q=80",
  "devialet-mania": "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=800&auto=format&fit=crop&q=80",
  "apple-watch-ultra-2": "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=800&auto=format&fit=crop&q=80",
  "galaxy-watch-ultra": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80",
  "garmin-fenix-8": "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800&auto=format&fit=crop&q=80",
  "ipad-pro-13-m4": "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&auto=format&fit=crop&q=80",
  "galaxy-tab-s10-ultra": "https://images.unsplash.com/photo-1561154464-82e9adf32764?w=800&auto=format&fit=crop&q=80",
  "keychron-q1-pro": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&auto=format&fit=crop&q=80",
  "anker-prime-magsafe": "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&auto=format&fit=crop&q=80",
  "anker-prime-charging-stand": "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&auto=format&fit=crop&q=80",
  "dyson-solarcycle-morph": "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&auto=format&fit=crop&q=80",
  "aerophone-16-pro-max": "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&auto=format&fit=crop&q=80",
  "chrono-horizon-master": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80",
  "opus-noir-sound-system": "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=800&auto=format&fit=crop&q=80",
  "lumina-carbon-earbuds": "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&auto=format&fit=crop&q=80",
  "aerobook-ultra-studio": "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop&q=80"
};

export function getProductDisplayImage(slug: string, backendImageUrl?: string | null): string | null {
  if (backendImageUrl && backendImageUrl.trim() !== "") {
    return backendImageUrl;
  }
  return DEFAULT_PRODUCT_IMAGES[slug] ?? null;
}
