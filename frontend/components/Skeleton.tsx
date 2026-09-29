export function SkeletonGrid() {
  return (
    <div className="product-grid" aria-label="Loading products">
      {Array.from({ length: 6 }, (_, index) => (
        <div className="product-card skeleton-card" key={index}>
          <div className="skeleton-media" />
          <div className="skeleton-line skeleton-wide" />
          <div className="skeleton-line" />
        </div>
      ))}
    </div>
  );
}
