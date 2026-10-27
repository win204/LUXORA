import Link from "next/link";
import { Container } from "./Container";
import { CartNavCount } from "./CartNavCount";
import { AuthNav } from "./AuthNav";

export function Navbar() {
  return (
    <header className="site-header">
      <div className="announcement-bar" aria-label="Special announcements">
        <div className="announcement-track">
          <span>Giao hàng đặc quyền toàn quốc — Đóng gói hộp quà cao cấp Maison</span>
          <span>Đặc quyền VIP — Tích lũy điểm thưởng &amp; thăng hạng tự động</span>
          <span>Voucher WELCOME50 — Giảm $50 cho đơn hàng từ $200</span>
          <span>Dịch vụ White-Glove Concierge — Tư vấn 24/7</span>
          <span>Giao hàng đặc quyền toàn quốc — Đóng gói hộp quà cao cấp Maison</span>
          <span>Đặc quyền VIP — Tích lũy điểm thưởng &amp; thăng hạng tự động</span>
          <span>Voucher WELCOME50 — Giảm $50 cho đơn hàng từ $200</span>
          <span>Dịch vụ White-Glove Concierge — Tư vấn 24/7</span>
          <span>Giao hàng đặc quyền toàn quốc — Đóng gói hộp quà cao cấp Maison</span>
          <span>Đặc quyền VIP — Tích lũy điểm thưởng &amp; thăng hạng tự động</span>
          <span>Voucher WELCOME50 — Giảm $50 cho đơn hàng từ $200</span>
          <span>Dịch vụ White-Glove Concierge — Tư vấn 24/7</span>
          <span>Giao hàng đặc quyền toàn quốc — Đóng gói hộp quà cao cấp Maison</span>
          <span>Đặc quyền VIP — Tích lũy điểm thưởng &amp; thăng hạng tự động</span>
          <span>Voucher WELCOME50 — Giảm $50 cho đơn hàng từ $200</span>
          <span>Dịch vụ White-Glove Concierge — Tư vấn 24/7</span>
        </div>
      </div>
      <Container className="nav">
        <Link className="brand-mark" href="/">
          <span className="brand-word">LUXORA</span>
          <span className="brand-sub">MAISON</span>
        </Link>
        <nav aria-label="Primary navigation">
          <Link className="nav-link" href="/shop">Bộ Sưu Tập</Link>
          <Link className="cart-link" href="/cart">
            <span>Túi mua sắm</span>
            <CartNavCount />
          </Link>
          <AuthNav />
        </nav>
      </Container>
    </header>
  );
}
