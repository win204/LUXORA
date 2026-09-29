"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { clearCart, getCart, removeCartItem, updateCartItem } from "@/lib/cartClient";
import type { Cart } from "@/lib/types";
import { EmptyState } from "./EmptyState";
import { ErrorState } from "./ErrorState";
import { Price } from "./Price";

export function CartView() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyItem, setBusyItem] = useState<string | null>(null);

  const loadCart = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setCart(await getCart());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Unable to load cart.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadCart);
  }, [loadCart]);

  async function handleQuantity(itemId: string, quantity: number) {
    if (quantity < 1) {
      return;
    }

    setBusyItem(itemId);
    setError("");
    try {
      setCart(await updateCartItem(itemId, quantity));
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Unable to update item.");
    } finally {
      setBusyItem(null);
    }
  }

  async function handleRemove(itemId: string) {
    setBusyItem(itemId);
    setError("");
    try {
      setCart(await removeCartItem(itemId));
    } catch (removeError) {
      setError(removeError instanceof Error ? removeError.message : "Unable to remove item.");
    } finally {
      setBusyItem(null);
    }
  }

  async function handleClear() {
    setError("");
    try {
      setCart(await clearCart());
    } catch (clearError) {
      setError(clearError instanceof Error ? clearError.message : "Unable to clear cart.");
    }
  }

  if (loading) {
    return <div className="state-panel">Loading cart...</div>;
  }

  if (error && !cart) {
    return <ErrorState message={error} />;
  }

  if (!cart || cart.items.length === 0) {
    return <EmptyState title="Your cart is empty" message="Explore the collection and add a selected variant when ready." />;
  }

  return (
    <div className="cart-layout">
      <section className="cart-items" aria-label="Cart items">
        {error ? <p className="form-message error">{error}</p> : null}
        {cart.items.map((item) => (
          <article className="cart-item" key={item.itemId}>
            <div>
              <Link href={`/products/${item.productSlug}`}>
                <h2>{item.productName}</h2>
              </Link>
              <p>{item.color ?? "Standard"} {item.storage ? `/ ${item.storage}` : ""}</p>
              <p className="meta-line">SKU {item.sku}</p>
            </div>
            <div className="cart-controls">
              <Price amount={item.unitPrice} />
              <label>
                <span>Quantity</span>
                <input
                  min="1"
                  type="number"
                  value={item.quantity}
                  disabled={busyItem === item.itemId}
                  onChange={(event) => handleQuantity(item.itemId, Number(event.target.value))}
                />
              </label>
              <Price amount={item.subtotal} />
              <button className="text-button" type="button" disabled={busyItem === item.itemId} onClick={() => handleRemove(item.itemId)}>
                Remove
              </button>
            </div>
          </article>
        ))}
      </section>

      <aside className="cart-summary" aria-label="Cart summary">
        <h2>Summary</h2>
        <div>
          <span>Total items</span>
          <strong>{cart.totalItems}</strong>
        </div>
        <div>
          <span>Subtotal</span>
          <Price amount={cart.subtotalTotal} />
        </div>
        <Link className="button button-primary" href="/checkout">
          Proceed to checkout
        </Link>
        <button className="button button-secondary" type="button" onClick={handleClear}>
          Clear cart
        </button>
      </aside>
    </div>
  );
}

