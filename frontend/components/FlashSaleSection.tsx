"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { getActiveFlashSale } from "@/lib/flashSaleClient";
import type { FlashSale } from "@/lib/types";
import { FlashSaleCountdown } from "@/components/FlashSaleCountdown";
import { Price } from "@/components/Price";
import { getProductDisplayImage } from "@/lib/fallbackProducts";

export function FlashSaleSection() {
  const [sale, setSale] = useState<FlashSale | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadSale() {
      try {
        const data = await getActiveFlashSale();
        if (active) setSale(data);
      } catch {
        if (active) setSale(null);
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadSale();
    return () => {
      active = false;
    };
  }, []);

  if (loading || !sale || sale.items.length === 0) {
    return null;
  }

  return (
    <section className="flash-sale-section" aria-label="Limited time flash sale">
      <div className="flash-sale-hero">
        <div className="flash-sale-header">
          <div className="flash-sale-title-block">
            <span className="flash-badge-luxury">⚡ LIMITED TIME ONLY</span>
            <h2>{sale.name}</h2>
            {sale.description ? <p className="flash-desc">{sale.description}</p> : null}
          </div>

          <div className="flash-timer-wrapper">
            <span className="flash-timer-label">OFFER EXPIRES IN</span>
            <FlashSaleCountdown
              initialSeconds={sale.timeRemainingSeconds}
              onExpire={() => setSale(null)}
            />
          </div>
        </div>

        <div className="flash-items-grid">
          {sale.items.map((item) => {
            const itemImage = getProductDisplayImage(item.productSlug, item.productImageUrl);
            return (
              <article key={item.id} className="flash-item-card">
                <div className="flash-item-media">
                  {itemImage ? (
                    <Image
                      src={itemImage}
                      alt={item.productName}
                      width={320}
                      height={320}
                      className="flash-item-img"
                      unoptimized
                    />
                  ) : (
                    <div className="flash-item-placeholder">LUXORA</div>
                  )}
                {item.discountPercent > 0 ? (
                  <span className="flash-discount-tag">-{item.discountPercent}%</span>
                ) : (
                  <span className="flash-discount-tag">SALE</span>
                )}
              </div>

              <div className="flash-item-body">
                <Link href={`/products/${item.productSlug}`} className="flash-item-title">
                  {item.productName}
                </Link>
                {item.variantName ? (
                  <span className="flash-variant-name">{item.variantName}</span>
                ) : null}

                <div className="flash-price-row">
                  <Price amount={String(item.flashSalePrice)} />
                  {item.originalPrice > item.flashSalePrice ? (
                    <span className="flash-original-price">
                      ${item.originalPrice.toFixed(2)}
                    </span>
                  ) : null}
                </div>

                <div className="flash-progress-block">
                  <div className="flash-progress-track">
                    <div
                      className="flash-progress-fill"
                      style={{ width: `${Math.min(100, Math.max(5, item.percentClaimed))}%` }}
                    />
                  </div>
                  <div className="flash-progress-meta">
                    <span>{item.percentClaimed}% Claimed</span>
                    <span className="flash-remaining-text">
                      {item.availableQuantity > 0 ? `${item.availableQuantity} left` : "Sold Out"}
                    </span>
                  </div>
                </div>

                <Link
                  href={`/products/${item.productSlug}`}
                  className={`flash-cta-button ${item.availableQuantity <= 0 ? "disabled" : ""}`}
                >
                  {item.availableQuantity > 0 ? "Claim Offer →" : "Sold Out"}
                </Link>
              </div>
            </article>
          )})}
        </div>
      </div>
    </section>
  );
}
