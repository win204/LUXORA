"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import Link from "next/link";
import { fetchActiveBanners } from "../lib/bannerClient";
import type { BannerResponse } from "../lib/types";

const DEFAULT_SLIDES: BannerResponse[] = [
  {
    id: "default-1",
    title: "Haute Horlogerie Grand Exhibition",
    subtitle: "Nghệ thuật chế tác đồng hồ Thụy Sỹ đỉnh cao — Cơ cấu chuyển động phức tạp & hoàn thiện thủ công từ các Maison di sản.",
    imageUrl: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1600&auto=format&fit=crop&q=80",
    linkUrl: "/shop",
    ctaText: "Khám phá đồng hồ",
    displayOrder: 1,
    active: true,
    startsAt: null,
    endsAt: null,
    createdAt: "",
    updatedAt: ""
  },
  {
    id: "default-2",
    title: "Celestial Diamond & High Jewelry",
    subtitle: "Tuyệt phẩm trang sức chế tác tinh xảo trên nền Platinum & vàng hồng 18K cùng kim cương thiên nhiên kiểm định quốc tế.",
    imageUrl: "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=1600&auto=format&fit=crop&q=80",
    linkUrl: "/shop",
    ctaText: "Khám phá bộ sưu tập",
    displayOrder: 2,
    active: true,
    startsAt: null,
    endsAt: null,
    createdAt: "",
    updatedAt: ""
  },
  {
    id: "default-3",
    title: "Private Salon & Bespoke Concierge",
    subtitle: "Dịch vụ đặc quyền thượng lưu — Trợ lý cá nhân 24/7, giao hàng White-Glove & tích điểm thăng hạng hội viên VIP.",
    imageUrl: "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=1600&auto=format&fit=crop&q=80",
    linkUrl: "/account",
    ctaText: "Đặc quyền thành viên",
    displayOrder: 3,
    active: true,
    startsAt: null,
    endsAt: null,
    createdAt: "",
    updatedAt: ""
  }
];

export function HeroBannerCarousel() {
  const [banners, setBanners] = useState<BannerResponse[]>(DEFAULT_SLIDES);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    fetchActiveBanners().then((res) => {
      if (res && res.length > 0) {
        setBanners(res);
      }
    });
  }, []);

  const total = banners.length;

  const nextSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % total);
  }, [total]);

  const prevSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 + total) % total);
  }, [total]);

  useEffect(() => {
    if (isPaused || total <= 1) return;
    timerRef.current = setInterval(nextSlide, 6000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [nextSlide, isPaused, total]);

  const current = banners[currentIndex] ?? DEFAULT_SLIDES[0];

  return (
    <section
      className={`lux-hero ${isPaused ? "is-paused" : ""}`}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      aria-label="Promotional Showcase"
    >
      {banners.map((slide, index) => {
        const isActive = index === currentIndex;
        return (
          <div
            key={slide.id}
            className={`lux-hero-slide ${isActive ? "is-active" : ""}`}
          >
            <div
              className="lux-hero-image"
              style={{ backgroundImage: `url(${slide.imageUrl})` }}
            />
            <div className="lux-hero-overlay" />
          </div>
        );
      })}

      <div className="lux-hero-content lux-hero-animate" key={current.id}>
        <div className="lux-hero-badge">
          <i />
          <span>MAISON LUXORA &bull; EXCLUSIVE SHOWCASE</span>
        </div>

        <h1>{current.title}</h1>

        {current.subtitle ? (
          <p className="lux-hero-sub">{current.subtitle}</p>
        ) : null}

        <div className="lux-hero-actions">
          <Link href={current.linkUrl || "/shop"} className="button button-primary">
            {current.ctaText || "Khám phá ngay"}
          </Link>
          <Link href="/shop" className="button button-secondary">
            Toàn bộ bộ sưu tập
          </Link>
        </div>
      </div>

      <div className="lux-hero-counter">
        <strong>0{currentIndex + 1}</strong>
        <span>/ 0{total}</span>
        <div className="lux-hero-progress">
          {banners.map((slide, index) => (
            <button
              key={slide.id}
              type="button"
              className={index === currentIndex ? "is-active" : index < currentIndex ? "is-done" : ""}
              onClick={() => setCurrentIndex(index)}
              aria-label={`Chuyển tới slide ${index + 1}`}
            >
              <span />
            </button>
          ))}
        </div>
      </div>

      {total > 1 ? (
        <div className="lux-hero-arrows">
          <button type="button" onClick={prevSlide} aria-label="Slide trước">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button type="button" onClick={nextSlide} aria-label="Slide tiếp theo">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      ) : null}
    </section>
  );
}
