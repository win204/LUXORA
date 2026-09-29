"use client";

import { useMemo, useState } from "react";
import { addCartItem } from "@/lib/cartClient";
import type { ProductVariant } from "@/lib/types";
import { Price } from "./Price";

type ProductVariantSelectorProps = {
  variants: ProductVariant[];
};

export function ProductVariantSelector({ variants }: ProductVariantSelectorProps) {
  const firstAvailable = useMemo(
    () => variants.find((variant) => variant.inStock) ?? variants[0],
    [variants]
  );
  const [selectedId, setSelectedId] = useState(firstAvailable?.id ?? "");
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [message, setMessage] = useState("");

  const selectedVariant = variants.find((variant) => variant.id === selectedId) ?? firstAvailable;
  const canAdd = Boolean(selectedVariant?.inStock && status !== "loading");

  async function handleAddToCart() {
    if (!selectedVariant) {
      return;
    }

    setStatus("loading");
    setMessage("");

    try {
      await addCartItem(selectedVariant.id, 1);
      setStatus("success");
      setMessage("Added to cart.");
    } catch (error) {
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "Unable to add item.");
    }
  }

  if (!selectedVariant) {
    return null;
  }

  return (
    <div className="purchase-panel">
      <div className="selected-variant">
        <div>
          <span>Selected price</span>
          <Price amount={selectedVariant.price} />
        </div>
        <div>
          <span>SKU</span>
          <strong>{selectedVariant.sku}</strong>
        </div>
        <div>
          <span>Availability</span>
          <strong className={selectedVariant.inStock ? "stock-text in" : "stock-text out"}>
            {selectedVariant.inStock ? "In stock" : "Out of stock"}
          </strong>
        </div>
      </div>

      <fieldset className="variant-picker">
        <legend>Variants</legend>
        {variants.map((variant) => (
          <label className={variant.id === selectedId ? "variant-option selected" : "variant-option"} key={variant.id}>
            <input
              type="radio"
              name="variant"
              value={variant.id}
              checked={variant.id === selectedId}
              disabled={!variant.inStock}
              onChange={() => setSelectedId(variant.id)}
            />
            <span>
              <strong>{variant.color ?? "Standard"}</strong>
              <small>{variant.storage ?? variant.sku}</small>
            </span>
            <Price amount={variant.price} />
          </label>
        ))}
      </fieldset>

      <button className="button button-primary add-cart-button" type="button" disabled={!canAdd} onClick={handleAddToCart}>
        {status === "loading" ? "Adding..." : "Add to cart"}
      </button>

      {message ? <p className={status === "error" ? "form-message error" : "form-message"}>{message}</p> : null}
    </div>
  );
}
