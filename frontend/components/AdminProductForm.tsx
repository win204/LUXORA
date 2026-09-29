"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button, LinkButton } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import {
  addAdminProductImage,
  addAdminProductSpecification,
  createAdminProduct,
  createAdminVariant,
  getAdminProduct,
  removeAdminProductImage,
  removeAdminProductSpecification,
  updateAdminInventory,
  updateAdminProduct,
  updateAdminVariant
} from "@/lib/adminClient";
import { getBrands, getCategories } from "@/lib/api";
import type {
  AdminProductDetail,
  AdminProductPayload,
  AdminProductVariant,
  AdminVariantUpdatePayload,
  Brand,
  Category
} from "@/lib/types";

const emptyProductForm: AdminProductPayload = {
  name: "",
  slug: "",
  subtitle: "",
  description: "",
  brandId: "",
  categoryId: "",
  active: true
};

const emptyVariantForm = {
  sku: "",
  color: "",
  storage: "",
  price: "0.00",
  active: true,
  quantityAvailable: 0
};

const emptyImageForm = { url: "", altText: "", displayOrder: 0 };
const emptySpecForm = { name: "", value: "", displayOrder: 0 };

export function AdminProductForm({ productId }: { productId?: string }) {
  const router = useRouter();
  const editing = Boolean(productId);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [product, setProduct] = useState<AdminProductDetail | null>(null);
  const [form, setForm] = useState<AdminProductPayload>(emptyProductForm);
  const [variantForm, setVariantForm] = useState(emptyVariantForm);
  const [imageForm, setImageForm] = useState(emptyImageForm);
  const [specForm, setSpecForm] = useState(emptySpecForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const [nextBrands, nextCategories, nextProduct] = await Promise.all([
          getBrands(),
          getCategories(),
          productId ? getAdminProduct(productId) : Promise.resolve(null)
        ]);
        if (!active) return;
        setBrands(nextBrands);
        setCategories(nextCategories);
        setProduct(nextProduct);
        if (nextProduct) {
          setForm({
            name: nextProduct.name,
            slug: nextProduct.slug,
            subtitle: nextProduct.subtitle ?? "",
            description: nextProduct.description,
            brandId: nextProduct.brandId,
            categoryId: nextProduct.categoryId,
            active: nextProduct.active
          });
        } else {
          setForm({
            ...emptyProductForm,
            brandId: nextBrands[0]?.id ?? "",
            categoryId: nextCategories[0]?.id ?? ""
          });
        }
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load product form.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [productId]);

  async function saveProduct() {
    if (!form.name.trim() || !form.slug.trim() || !form.description.trim() || !form.brandId || !form.categoryId) {
      setError("Name, slug, description, brand, and category are required.");
      return;
    }
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(form.slug)) {
      setError("Slug must be lowercase words separated by hyphens.");
      return;
    }
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const next = productId ? await updateAdminProduct(productId, form) : await createAdminProduct(form);
      setProduct(next);
      setSuccess(productId ? "Product updated." : "Product created.");
      if (!productId) router.replace(`/admin/products/${next.id}`);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to save product.");
    } finally {
      setSaving(false);
    }
  }

  async function addVariant() {
    if (!product || !variantForm.sku.trim() || Number(variantForm.price) < 0 || variantForm.quantityAvailable < 0) {
      setError("Variant SKU, non-negative price, and non-negative stock are required.");
      return;
    }
    await mutateProduct(() => createAdminVariant(product.id, variantForm), "Variant added.");
    setVariantForm(emptyVariantForm);
  }

  async function saveVariant(variantId: string, payload: AdminVariantUpdatePayload) {
    if (!payload.sku.trim() || Number(payload.price) < 0) {
      setError("Variant SKU and non-negative price are required.");
      return;
    }
    await mutateProduct(() => updateAdminVariant(variantId, payload), "Variant updated.");
  }

  async function saveInventory(variantId: string, quantityAvailable: number) {
    if (quantityAvailable < 0) {
      setError("Inventory quantity cannot be negative.");
      return;
    }
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const nextVariant = await updateAdminInventory(variantId, { quantityAvailable });
      setProduct((current) => current ? { ...current, variants: current.variants.map((variant) => variant.id === variantId ? nextVariant : variant) } : current);
      setSuccess("Inventory updated.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to update inventory.");
    } finally {
      setSaving(false);
    }
  }

  async function addImage() {
    if (!product || !imageForm.url.trim()) {
      setError("Image URL is required.");
      return;
    }
    await mutateProduct(() => addAdminProductImage(product.id, imageForm), "Image added.");
    setImageForm(emptyImageForm);
  }

  async function addSpecification() {
    if (!product || !specForm.name.trim() || !specForm.value.trim()) {
      setError("Specification name and value are required.");
      return;
    }
    await mutateProduct(() => addAdminProductSpecification(product.id, specForm), "Specification added.");
    setSpecForm(emptySpecForm);
  }

  async function mutateProduct(action: () => Promise<AdminProductDetail>, message: string) {
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setProduct(await action());
      setSuccess(message);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Admin product request failed.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <div className="admin-heading admin-heading-row">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>{editing ? "Edit product" : "New product"}</h1>
        </div>
        <LinkButton href="/admin/products" variant="secondary">Products</LinkButton>
      </div>

      {loading ? <div className="state-panel" aria-busy="true">Loading product...</div> : null}
      {error ? <ErrorState title="Product request failed" message={error} /> : null}
      {success ? <div className="success-panel">{success}</div> : null}

      {!loading ? (
        <div className="admin-form-stack">
          <section className="admin-panel">
            <h2>Product information</h2>
            <div className="admin-form-grid">
              <Field label="Name" value={form.name} onChange={(value) => setForm({ ...form, name: value })} />
              <Field label="Slug" value={form.slug} onChange={(value) => setForm({ ...form, slug: value })} />
              <Field label="Subtitle" value={form.subtitle ?? ""} onChange={(value) => setForm({ ...form, subtitle: value })} />
              <label className="field-label">Brand<select value={form.brandId} onChange={(event) => setForm({ ...form, brandId: event.target.value })}>{brands.map((brand) => <option key={brand.id} value={brand.id}>{brand.name}</option>)}</select></label>
              <label className="field-label">Category<select value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
              <label className="field-check"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Active</label>
              <label className="field-label field-wide">Description<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} rows={5} /></label>
            </div>
            <Button type="button" onClick={saveProduct} disabled={saving}>{saving ? "Saving..." : "Save product"}</Button>
          </section>

          {product ? (
            <>
              <section className="admin-panel">
                <h2>Variants and inventory</h2>
                <div className="admin-collection">
                  {product.variants.map((variant) => <VariantEditor key={`${variant.id}-${variant.sku}-${variant.quantityAvailable}-${variant.price}-${variant.active}`} variant={variant} disabled={saving} onSave={saveVariant} onInventory={saveInventory} />)}
                </div>
                <div className="admin-inline-form">
                  <Field label="SKU" value={variantForm.sku} onChange={(value) => setVariantForm({ ...variantForm, sku: value })} />
                  <Field label="Color" value={variantForm.color} onChange={(value) => setVariantForm({ ...variantForm, color: value })} />
                  <Field label="Storage" value={variantForm.storage} onChange={(value) => setVariantForm({ ...variantForm, storage: value })} />
                  <Field label="Price" value={variantForm.price} type="number" onChange={(value) => setVariantForm({ ...variantForm, price: value })} />
                  <Field label="Stock" value={String(variantForm.quantityAvailable)} type="number" onChange={(value) => setVariantForm({ ...variantForm, quantityAvailable: Number(value) })} />
                  <label className="field-check"><input type="checkbox" checked={variantForm.active} onChange={(event) => setVariantForm({ ...variantForm, active: event.target.checked })} /> Active</label>
                  <Button type="button" onClick={addVariant} disabled={saving}>Add variant</Button>
                </div>
              </section>

              <section className="admin-panel">
                <h2>Images</h2>
                <div className="admin-collection compact">
                  {product.images.map((image) => <div className="admin-list-item" key={image.id}><span><strong>{image.url}</strong><small>{image.altText ?? "No alt text"}</small></span><Button type="button" variant="secondary" disabled={saving} onClick={() => mutateProduct(() => removeAdminProductImage(product.id, image.id), "Image removed.")}>Remove</Button></div>)}
                </div>
                <div className="admin-inline-form">
                  <Field label="Image URL" value={imageForm.url} onChange={(value) => setImageForm({ ...imageForm, url: value })} />
                  <Field label="Alt text" value={imageForm.altText} onChange={(value) => setImageForm({ ...imageForm, altText: value })} />
                  <Field label="Order" value={String(imageForm.displayOrder)} type="number" onChange={(value) => setImageForm({ ...imageForm, displayOrder: Number(value) })} />
                  <Button type="button" onClick={addImage} disabled={saving}>Add image</Button>
                </div>
              </section>

              <section className="admin-panel">
                <h2>Specifications</h2>
                <div className="admin-collection compact">
                  {product.specifications.map((spec) => <div className="admin-list-item" key={spec.id}><span><strong>{spec.name}</strong><small>{spec.value}</small></span><Button type="button" variant="secondary" disabled={saving} onClick={() => mutateProduct(() => removeAdminProductSpecification(product.id, spec.id), "Specification removed.")}>Remove</Button></div>)}
                </div>
                <div className="admin-inline-form">
                  <Field label="Name" value={specForm.name} onChange={(value) => setSpecForm({ ...specForm, name: value })} />
                  <Field label="Value" value={specForm.value} onChange={(value) => setSpecForm({ ...specForm, value })} />
                  <Field label="Order" value={String(specForm.displayOrder)} type="number" onChange={(value) => setSpecForm({ ...specForm, displayOrder: Number(value) })} />
                  <Button type="button" onClick={addSpecification} disabled={saving}>Add specification</Button>
                </div>
              </section>

              <Link href={`/products/${product.slug}`} className="admin-public-link">View public product</Link>
            </>
          ) : null}
        </div>
      ) : null}
    </AdminShell>
  );
}

function VariantEditor({ variant, disabled, onSave, onInventory }: { variant: AdminProductVariant; disabled: boolean; onSave: (id: string, payload: AdminVariantUpdatePayload) => Promise<void>; onInventory: (id: string, quantityAvailable: number) => Promise<void>; }) {
  const [draft, setDraft] = useState({ sku: variant.sku, color: variant.color ?? "", storage: variant.storage ?? "", price: variant.price, active: variant.active });
  const [quantity, setQuantity] = useState(variant.quantityAvailable);

  return (
    <div className="admin-variant-card">
      <Field label="SKU" value={draft.sku} onChange={(value) => setDraft({ ...draft, sku: value })} />
      <Field label="Color" value={draft.color} onChange={(value) => setDraft({ ...draft, color: value })} />
      <Field label="Storage" value={draft.storage} onChange={(value) => setDraft({ ...draft, storage: value })} />
      <Field label="Price" value={draft.price} type="number" onChange={(value) => setDraft({ ...draft, price: value })} />
      <Field label="Stock" value={String(quantity)} type="number" onChange={(value) => setQuantity(Number(value))} />
      <label className="field-check"><input type="checkbox" checked={draft.active} onChange={(event) => setDraft({ ...draft, active: event.target.checked })} /> Active</label>
      <div className="admin-actions-inline"><Button type="button" variant="secondary" disabled={disabled} onClick={() => onSave(variant.id, draft)}>Save SKU</Button><Button type="button" variant="secondary" disabled={disabled} onClick={() => onInventory(variant.id, quantity)}>Save stock</Button></div>
    </div>
  );
}

function Field({ label, value, type = "text", onChange }: { label: string; value: string; type?: string; onChange: (value: string) => void }) {
  return <label className="field-label">{label}<input type={type} value={value} min={type === "number" ? 0 : undefined} step={type === "number" ? "0.01" : undefined} onChange={(event) => onChange(event.target.value)} /></label>;
}