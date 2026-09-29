import { Container } from "@/components/Container";

export default function ProductLoading() {
  return (
    <main className="page">
      <Container className="detail-layout">
        <div className="gallery">
          <div className="detail-image skeleton-media" />
          <div className="detail-image skeleton-media" />
        </div>
        <div className="product-summary">
          <div className="skeleton-line skeleton-wide" />
          <div className="skeleton-line" />
          <div className="skeleton-line" />
        </div>
      </Container>
    </main>
  );
}
