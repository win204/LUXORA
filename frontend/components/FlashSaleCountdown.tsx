"use client";

import { useEffect, useState } from "react";

interface FlashSaleCountdownProps {
  initialSeconds: number;
  onExpire?: () => void;
}

export function FlashSaleCountdown({ initialSeconds, onExpire }: FlashSaleCountdownProps) {
  const [secondsRemaining, setSecondsRemaining] = useState<number>(initialSeconds);

  useEffect(() => {
    if (secondsRemaining <= 0) {
      if (onExpire) onExpire();
      return;
    }

    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          if (onExpire) onExpire();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [secondsRemaining, onExpire]);

  const hours = Math.floor(secondsRemaining / 3600);
  const minutes = Math.floor((secondsRemaining % 3600) / 60);
  const seconds = secondsRemaining % 60;

  const pad = (num: number) => String(num).padStart(2, "0");

  return (
    <div className="flash-countdown-container" aria-label="Flash sale time remaining">
      <div className="countdown-pill">
        <span className="countdown-num">{pad(hours)}</span>
        <span className="countdown-unit">HRS</span>
      </div>
      <span className="countdown-divider">:</span>
      <div className="countdown-pill">
        <span className="countdown-num">{pad(minutes)}</span>
        <span className="countdown-unit">MIN</span>
      </div>
      <span className="countdown-divider">:</span>
      <div className="countdown-pill">
        <span className="countdown-num">{pad(seconds)}</span>
        <span className="countdown-unit">SEC</span>
      </div>
    </div>
  );
}
