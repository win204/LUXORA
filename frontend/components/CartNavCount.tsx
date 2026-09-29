"use client";

import { useEffect, useState } from "react";
import { getAccessToken } from "@/lib/authClient";
import { getCart, getStoredCartId } from "@/lib/cartClient";

export function CartNavCount() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadCount() {
      if (!getStoredCartId() && !getAccessToken()) {
        setCount(0);
        return;
      }

      try {
        const cart = await getCart();
        if (active) {
          setCount(cart.totalItems);
        }
      } catch {
        if (active) {
          setCount(0);
        }
      }
    }

    loadCount();
    window.addEventListener("luxora-cart-updated", loadCount);
    window.addEventListener("luxora-auth-updated", loadCount);
    return () => {
      active = false;
      window.removeEventListener("luxora-cart-updated", loadCount);
      window.removeEventListener("luxora-auth-updated", loadCount);
    };
  }, []);

  return <span className="cart-count" aria-label={`${count} items in cart`}>{count}</span>;
}
