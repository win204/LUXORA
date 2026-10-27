import type { Metadata } from "next";
import Link from "next/link";
import { Container } from "@/components/Container";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { ProductGrid } from "@/components/ProductGrid";
import { FlashSaleSection } from "@/components/FlashSaleSection";
import { HeroBannerCarousel } from "@/components/HeroBannerCarousel";
import { getProducts } from "@/lib/api";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "LUXORA — Maison of Refined Technology",
  description: "Tuyển tập thiết bị công nghệ & đồng hồ di sản đỉnh cao, dịch vụ đặc quyền thượng lưu và bảo hành chuẩn quốc tế."
};

export default async function Home() {
  const productsResult = await getFeaturedProducts();

  return (
    <main>
      {/* Hero Banner Carousel */}
      <Container className="pt-2">
        <HeroBannerCarousel />
      </Container>

      {/* Trust & Heritage Marquee Strip */}
      <div className="trust-strip" aria-hidden="true">
        <div className="trust-track">
          <span>HAUTE TECHNOLOGIE</span>
          <span>100% KIỂM ĐỊNH NGUYÊN BẢN</span>
          <span>WHITE-GLOVE CONCIERGE</span>
          <span>BẢO HÀNH CHÍNH HÃNG 24 THÁNG</span>
          <span>ĐẶC QUYỀN HỘI VIÊN VIP</span>
          <span>HAUTE TECHNOLOGIE</span>
          <span>100% KIỂM ĐỊNH NGUYÊN BẢN</span>
          <span>WHITE-GLOVE CONCIERGE</span>
          <span>BẢO HÀNH CHÍNH HÃNG 24 THÁNG</span>
          <span>ĐẶC QUYỀN HỘI VIÊN VIP</span>
          <span>HAUTE TECHNOLOGIE</span>
          <span>100% KIỂM ĐỊNH NGUYÊN BẢN</span>
          <span>WHITE-GLOVE CONCIERGE</span>
          <span>BẢO HÀNH CHÍNH HÃNG 24 THÁNG</span>
          <span>ĐẶC QUYỀN HỘI VIÊN VIP</span>
          <span>HAUTE TECHNOLOGIE</span>
          <span>100% KIỂM ĐỊNH NGUYÊN BẢN</span>
          <span>WHITE-GLOVE CONCIERGE</span>
          <span>BẢO HÀNH CHÍNH HÃNG 24 THÁNG</span>
          <span>ĐẶC QUYỀN HỘI VIÊN VIP</span>
        </div>
      </div>

      {/* Flash Sale Event Section */}
      <Container>
        <FlashSaleSection />
      </Container>

      {/* Featured Collection Grid */}
      <section className="section">
        <Container>
          <div className="section-heading section-heading-row">
            <div>
              <p className="eyebrow">Tuyển Tập Nổi Bật</p>
              <h2>Bộ Sưu Tập <em>Di Sản</em></h2>
            </div>
            <Link href="/shop" className="section-link">
              <span>Xem toàn bộ 22 sản phẩm</span>
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </Link>
          </div>

          {productsResult.ok ? (
            productsResult.products.length > 0 ? (
              <ProductGrid products={productsResult.products} />
            ) : (
              <EmptyState title="Chưa có sản phẩm" message="Danh mục sản phẩm đang được cập nhật." />
            )
          ) : (
            <ErrorState message={productsResult.message} />
          )}
        </Container>
      </section>

      {/* Editorial Curations */}
      <section className="section" style={{ paddingTop: 0 }}>
        <Container>
          <div className="section-heading">
            <p className="eyebrow">Lookbook Chuyên Sâu</p>
            <h2>Nghệ Thuật <em>Chế Tác</em> Tinh Tế</h2>
          </div>

          <div className="editorial-grid">
            <div
              className="editorial-card"
              style={{
                backgroundImage:
                  "url('https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1200&auto=format&fit=crop&q=80')"
              }}
            >
              <div className="editorial-card-bg" />
              <p className="eyebrow" style={{ color: "#e6c887" }}>Haute Horlogerie</p>
              <h3>Đồng Hồ Di Sản &amp; Tuyệt Tác Cơ Khí</h3>
              <p>
                Hoàn thiện thủ công từ các xưởng chế tác danh giá, từng chi tiết là một minh chứng
                cho độ chuẩn xác vĩnh cửu.
              </p>
              <div>
                <Link href="/shop?category=wearables" className="button button-primary">
                  Khám phá đồng hồ
                </Link>
              </div>
            </div>

            <div className="editorial-stack">
              <div
                className="editorial-card"
                style={{
                  backgroundImage:
                    "url('https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=1000&auto=format&fit=crop&q=80')"
                }}
              >
                <div className="editorial-card-bg" />
                <p className="eyebrow" style={{ color: "#e6c887" }}>Acoustic Masterclass</p>
                <h3>Âm Thanh Phòng Thu Audiophile</h3>
                <p>Màng loa titanium chuẩn xác, truyền tải trọn vẹn từng rung cảm âm thanh.</p>
                <div>
                  <Link href="/shop?category=audio" className="button button-secondary">
                    Thiết bị âm thanh
                  </Link>
                </div>
              </div>

              <div
                className="editorial-card"
                style={{
                  backgroundImage:
                    "url('https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=1000&auto=format&fit=crop&q=80')"
                }}
              >
                <div className="editorial-card-bg" />
                <p className="eyebrow" style={{ color: "#e6c887" }}>Titanium &amp; Silicon</p>
                <h3>Công Nghệ Di Động Flagship</h3>
                <p>Khung vỏ Titanium hàng không cùng vi xử lý thế hệ mới nhất.</p>
                <div>
                  <Link href="/shop?category=laptops" className="button button-secondary">
                    Xem máy tính cao cấp
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </Container>
      </section>

      {/* Privilege & VIP Loyalty Club Showcase */}
      <section className="section" style={{ paddingTop: 0 }}>
        <Container>
          <div className="privilege">
            <div className="privilege-grid">
              <div>
                <p className="eyebrow">Đặc Quyền Thành Viên</p>
                <h2>Câu Lạc Bộ <em>LUXORA Privilege</em></h2>
                <p>
                  Mỗi giao dịch tại LUXORA đều tích lũy điểm thưởng và đưa quý khách đến với
                  các đặc quyền thượng lưu: cố vấn cá nhân, ưu đãi riêng tư và giao hàng White-Glove.
                </p>

                <div className="tier-chips">
                  <span className="tier-chip silver">SILVER &bull; 1.0X</span>
                  <span className="tier-chip gold">GOLD &bull; 1.25X VIP</span>
                  <span className="tier-chip platinum">PLATINUM &bull; 1.5X FREESHIP</span>
                </div>

                <div className="hero-actions">
                  <Link href="/account" className="button button-primary">
                    Xem hạng thành viên của bạn
                  </Link>
                  <Link href="/shop" className="button button-secondary">
                    Tích điểm ngay hôm nay
                  </Link>
                </div>
              </div>

              <div className="stat-grid">
                <div className="stat">
                  <strong>100%</strong>
                  <span>Kiểm Định Nguyên Bản</span>
                </div>
                <div className="stat">
                  <strong>24/7</strong>
                  <span>Private Concierge</span>
                </div>
                <div className="stat">
                  <strong>1.5x</strong>
                  <span>Hệ Số Điểm Platinum</span>
                </div>
                <div className="stat">
                  <strong>14 Ngày</strong>
                  <span>Đổi Trả Đặc Quyền</span>
                </div>
              </div>
            </div>
          </div>
        </Container>
      </section>

      {/* Pillars of Maison Services */}
      <section className="section" style={{ paddingTop: 0, paddingBottom: 64 }}>
        <Container>
          <div className="service-row">
            <div className="service">
              <div className="service-icon" aria-hidden="true">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
                </svg>
              </div>
              <h3>Giao Hàng White-Glove</h3>
              <p>Đóng gói hộp quà Maison sang trọng, niêm phong bảo mật và giao tận tay quý khách.</p>
            </div>

            <div className="service">
              <div className="service-icon" aria-hidden="true">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <h3>100% Chính Hãng</h3>
              <p>Tất cả sản phẩm đều có mã chứng nhận nguồn gốc và bảo hành chính hãng 24 tháng.</p>
            </div>

            <div className="service">
              <div className="service-icon" aria-hidden="true">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3>Thanh Toán An Toàn</h3>
              <p>Tích hợp cổng VNPay sandbox mã hoá SHA512 cùng nhiều phương thức thanh toán linh hoạt.</p>
            </div>

            <div className="service">
              <div className="service-icon" aria-hidden="true">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </div>
              <h3>Đổi Trả Trong 14 Ngày</h3>
              <p>Hỗ trợ tạo đơn trả hàng RMA trực tuyến thuận tiện và hoàn tiền minh bạch, nhanh chóng.</p>
            </div>
          </div>
        </Container>
      </section>
    </main>
  );
}

async function getFeaturedProducts() {
  try {
    const page = await getProducts({ size: "6", sort: "nameAsc" });
    return { ok: true as const, products: page.content };
  } catch (error) {
    return {
      ok: false as const,
      message: error instanceof Error ? error.message : "The catalog could not be loaded."
    };
  }
}
