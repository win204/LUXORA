"use client";

import Link from "next/link";
import { Container } from "./Container";

export function Footer() {
  return (
    <footer className="site-footer">
      <Container>
        <div className="footer-grid">
          <div className="footer-brand">
            <Link className="brand-mark" href="/">
              <span className="brand-word">LUXORA</span>
              <span className="brand-sub">MAISON</span>
            </Link>
            <p>
              Maison công nghệ tinh hoa — Nơi hội tụ các tuyệt tác công nghệ hàng đầu thế giới,
              chế tác tinh xảo cùng dịch vụ đặc quyền thượng lưu.
            </p>
            <form className="footer-newsletter" onSubmit={(e) => e.preventDefault()}>
              <input
                type="email"
                placeholder="Nhập email để nhận đặc quyền VIP..."
                aria-label="Email nhận bản tin"
              />
              <button type="submit" className="button button-primary">
                Tham gia
              </button>
            </form>
          </div>

          <div className="footer-col">
            <h4>Tuyển Tập</h4>
            <ul>
              <li><Link href="/shop">Toàn bộ sản phẩm</Link></li>
              <li><Link href="/shop?category=phones">Điện thoại di sản</Link></li>
              <li><Link href="/shop?category=laptops">Máy tính xách tay</Link></li>
              <li><Link href="/shop?category=audio">Thiết bị âm thanh</Link></li>
              <li><Link href="/shop?category=wearables">Đồng hồ thông minh</Link></li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Đặc Quyền</h4>
            <ul>
              <li><Link href="/account">Hội viên VIP Rewards</Link></li>
              <li><Link href="/account/orders">Theo dõi vận chuyển</Link></li>
              <li><Link href="/account/returns">Chính sách đổi trả 14 ngày</Link></li>
              <li><Link href="/shop">Flash Sale giới hạn</Link></li>
              <li><a href="#concierge">Dịch vụ White-Glove Concierge</a></li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Về Maison</h4>
            <ul>
              <li><a href="#heritage">Triết lý thiết kế</a></li>
              <li><a href="#boutique">Flagship Boutique</a></li>
              <li><a href="#contact">Liên hệ cố vấn</a></li>
              <li><a href="#press">Thông cáo báo chí</a></li>
              <li><Link href="/admin">Cổng quản trị viên</Link></li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} LUXORA Commerce Maison. Bản quyền được bảo lưu.</p>
          <div className="pay-badges">
            <span>VNPAY QR</span>
            <span>256-BIT SSL</span>
            <span>WHITE-GLOVE</span>
            <span>AUTHENTIC LUXURY</span>
          </div>
        </div>
      </Container>
    </footer>
  );
}
