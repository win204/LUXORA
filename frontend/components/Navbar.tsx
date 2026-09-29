import Link from "next/link";
import { Container } from "./Container";
import { CartNavCount } from "./CartNavCount";
import { AuthNav } from "./AuthNav";

export function Navbar() {
  return (
    <header className="site-header">
      <Container className="nav">
        <Link className="brand-mark" href="/">
          LUXORA
        </Link>
        <nav aria-label="Primary navigation">
          <Link href="/shop">Shop</Link>
          <Link className="cart-link" href="/cart">
            Cart <CartNavCount />
          </Link>
          <AuthNav />
        </nav>
      </Container>
    </header>
  );
}
