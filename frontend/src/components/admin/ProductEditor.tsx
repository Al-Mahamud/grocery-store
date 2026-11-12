"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ExternalLink, ImageUp, Plus, Save, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { AdminPack, AdminProduct, ProductStatus, Unit } from "@/lib/admin-types";
import type { CategoryNode } from "@/lib/types";
import { Card, btn, input } from "./ui";
import { useAdmin } from "./AdminShell";

type Form = Omit<AdminProduct, "id" | "category" | "image" | "soldCount" | "categoryId" | "localName" | "brand" | "storage" | "origin"> & {
  categoryId: string; localName: string; brand: string; storage: string; origin: string;
};

const units: { value: Unit; label: string }[] = [
  { value: "G", label: "g" }, { value: "KG", label: "kg" }, { value: "ML", label: "ml" }, { value: "L", label: "L" }, { value: "PCS", label: "pcs" },
];

const blankPack = (): AdminPack => ({ id: null, label: "", amount: "", unit: "KG", sku: "", price: "", compareAtPrice: "", stock: 0, maxPerOrder: 20 });

function toForm(p: AdminProduct | null): Form {
  return {
    name: p?.name ?? "", localName: p?.localName ?? "", slug: p?.slug ?? "", categoryId: p ? String(p.categoryId) : "",
    brand: p?.brand ?? "", description: p?.description ?? "", storage: p?.storage ?? "", origin: p?.origin ?? "",
    organic: p?.organic ?? false, local: p?.local ?? false, fresh: p?.fresh ?? false, weighed: p?.weighed ?? false,
    featured: p?.featured ?? false, status: p?.status ?? "DRAFT",
    packs: p?.packs.map((k) => ({ ...k, compareAtPrice: k.compareAtPrice ?? "" })) ?? [blankPack()],
  };
}

const num = (v: number | string | null) => (v === "" || v === null ? null : Number(v));

/* eslint-disable @next/next/no-img-element */
export function ProductEditor({ id }: { id: number | null }) {
  const router = useRouter();
  const { notify } = useAdmin();
  const [product, setProduct] = useState<AdminProduct | null>(null);
  const [form, setForm] = useState<Form>(toForm(null));
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [loaded, setLoaded] = useState(id === null);
  const file = useRef<HTMLInputElement>(null);

  useEffect(() => {
    request<CategoryNode[]>("GET", "/categories").then(setCategories).catch(() => undefined);
    if (id === null) return;
    request<AdminProduct>("GET", `/admin/products/${id}`).then((p) => {
      setProduct(p);
      setForm(toForm(p));
      setLoaded(true);
    });
  }, [id]);

  const set = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: "" }));
  };
  const setPack = (i: number, key: keyof AdminPack, value: string | number) => {
    setForm((f) => ({ ...f, packs: f.packs.map((p, n) => (n === i ? { ...p, [key]: value } : p)) }));
    setErrors((e) => ({ ...e, [`packs.${i}.${key}`]: "" }));
  };

  const save = async () => {
    setBusy(true);
    try {
      const body = {
        ...form,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        packs: form.packs.map((p) => ({ ...p, amount: num(p.amount), price: num(p.price), compareAtPrice: num(p.compareAtPrice), stock: num(p.stock) ?? 0, maxPerOrder: num(p.maxPerOrder) ?? 20 })),
      };
      const saved = await request<AdminProduct>(id ? "PUT" : "POST", id ? `/admin/products/${id}` : "/admin/products", body);
      setErrors({});
      notify("Product saved");
      if (!id) router.replace(`/admin/products/${saved.id}`);
      else {
        setProduct(saved);
        setForm(toForm(saved));
      }
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields);
        notify(e.message);
      } else notify("Couldn't save. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  const upload = async (f: File) => {
    if (!id) return;
    const data = new FormData();
    data.append("file", f);
    const token = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="))?.slice(11) ?? "";
    const response = await fetch(`/api/v1/admin/products/${id}/image`, { method: "POST", body: data, headers: { "X-XSRF-TOKEN": decodeURIComponent(token) } });
    const json = await response.json().catch(() => null);
    if (!response.ok) {
      notify(json?.message ?? "Couldn't upload the picture.");
      return;
    }
    setProduct(json as AdminProduct);
    notify("Picture updated");
  };

  if (!loaded) return <div className="h-64 animate-pulse rounded-2xl bg-white" />;
  const err = (key: string) => errors[key] ? <span className="mt-0.5 block text-xs font-semibold text-berry">{errors[key]}</span> : null;
  const field = (label: string, key: keyof Form, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">{label}</span>
      <input value={form[key] as string} onChange={(e) => set(key, e.target.value as never)} className={`${input} ${errors[key] ? "border-berry" : ""}`} {...props} />{err(key)}</label>
  );

  return (
    <div className="space-y-4 pb-24">
      <Link href="/admin/products" className="inline-flex items-center gap-1.5 text-sm font-bold text-ink-soft hover:text-ink"><ArrowLeft size={15} /> Products</Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-extrabold tracking-tight">{id ? form.name || "Product" : "New product"}</h1>
        {product?.status === "ACTIVE" && <Link href={`/p/${product.slug}`} target="_blank" className={btn.ghost}><ExternalLink size={15} /> View in shop</Link>}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Details" className="lg:col-span-2">
          <div className="grid gap-3 sm:grid-cols-2">
            {field("Name", "name", { placeholder: "e.g. Red Amaranth" })}
            {field("Bangla name", "localName", { placeholder: "e.g. লাল শাক", className: `${input} font-bangla` })}
            <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">Category</span>
              <select value={form.categoryId} onChange={(e) => set("categoryId", e.target.value)} className={`${input} ${errors.categoryId ? "border-berry" : ""}`}>
                <option value="">Choose…</option>
                {categories.map((top) => <optgroup key={top.id} label={top.name}>{top.children.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</optgroup>)}
              </select>{err("categoryId")}</label>
            {field("Brand", "brand", { placeholder: "Optional" })}
            <label className="block sm:col-span-2"><span className="mb-1 block text-xs font-bold text-ink-soft">Description</span>
              <textarea rows={3} value={form.description} onChange={(e) => set("description", e.target.value)} className={input} /></label>
            {field("Storage", "storage", { placeholder: "e.g. Keep refrigerated" })}
            {field("Origin", "origin", { placeholder: "e.g. Bogura" })}
            {field("Web address", "slug", { placeholder: "Made from the name if left empty" })}
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {([["organic", "Organic"], ["local", "Local"], ["fresh", "Fresh today"], ["weighed", "Sold by weight"], ["featured", "Featured"]] as const).map(([key, label]) => (
              <label key={key} className={`flex cursor-pointer items-center gap-2 rounded-xl border-2 px-3 py-1.5 text-sm font-semibold ${form[key] ? "border-ink bg-ink text-white" : "border-line"}`}>
                <input type="checkbox" className="sr-only" checked={form[key]} onChange={(e) => set(key, e.target.checked)} /> {label}
              </label>
            ))}
          </div>
        </Card>
        <div className="space-y-4">
          <Card title="Status">
            <div className="grid gap-1.5">
              {(["ACTIVE", "DRAFT", "ARCHIVED"] as ProductStatus[]).map((s) => (
                <label key={s} className={`flex cursor-pointer items-center gap-2 rounded-xl border-2 px-3 py-2 text-sm ${form.status === s ? "border-tangerine bg-tangerine-soft" : "border-line"}`}>
                  <input type="radio" name="status" className="accent-tangerine" checked={form.status === s} onChange={() => set("status", s)} />
                  <span><strong>{{ ACTIVE: "On sale", DRAFT: "Draft", ARCHIVED: "Archived" }[s]}</strong><span className="block text-xs text-muted">{{ ACTIVE: "Shoppers can find and buy it", DRAFT: "Hidden while you prepare it", ARCHIVED: "No longer sold; kept for old orders" }[s]}</span></span>
                </label>
              ))}
            </div>
          </Card>
          <Card title="Picture">
            <div className="grid aspect-square place-items-center overflow-hidden rounded-xl bg-cream">
              {product?.image ? <img src={product.image} alt="" className="h-full w-full object-contain" /> : <span className="text-sm text-muted">No picture yet</span>}
            </div>
            <input ref={file} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) void upload(f); e.target.value = ""; }} />
            <button type="button" disabled={!id} onClick={() => file.current?.click()} className={`${btn.ghost} mt-3 w-full`}><ImageUp size={15} /> {id ? "Upload JPEG, PNG or WebP" : "Save first, then add a picture"}</button>
          </Card>
        </div>
      </div>

      <Card title="Packs" action={<button type="button" onClick={() => set("packs", [...form.packs, blankPack()])} className={btn.ghost}><Plus size={15} /> Add pack</button>}>
        {errors.packs && <p className="mb-2 text-sm font-semibold text-berry">{errors.packs}</p>}
        <div className="space-y-3">
          {form.packs.map((p, i) => (
            <div key={p.id ?? `new-${i}`} className="grid grid-cols-2 gap-2 rounded-xl border border-line p-3 sm:grid-cols-4 xl:grid-cols-[1.3fr_0.7fr_0.6fr_1fr_0.8fr_0.8fr_0.7fr_0.7fr_auto]">
              {([["Pack name", "label", "1 kg"], ["Amount", "amount", "1"]] as const).map(([label, key, ph]) => (
                <label key={key} className="block"><span className="mb-1 block text-[0.6875rem] font-bold text-ink-soft">{label}</span><input value={p[key] as string} placeholder={ph} onChange={(e) => setPack(i, key, e.target.value)} className={`${input} ${errors[`packs.${i}.${key}`] ? "border-berry" : ""}`} />{err(`packs.${i}.${key}`)}</label>
              ))}
              <label className="block"><span className="mb-1 block text-[0.6875rem] font-bold text-ink-soft">Unit</span>
                <select value={p.unit} onChange={(e) => setPack(i, "unit", e.target.value)} className={input}>{units.map((u) => <option key={u.value} value={u.value}>{u.label}</option>)}</select></label>
              {([["SKU", "sku", "VEG-001"], ["Price ৳", "price", ""], ["Old price ৳", "compareAtPrice", "optional"], ["Stock", "stock", "0"], ["Max / order", "maxPerOrder", "20"]] as const).map(([label, key, ph]) => (
                <label key={key} className="block"><span className="mb-1 block text-[0.6875rem] font-bold text-ink-soft">{label}</span><input value={(p[key] ?? "") as string} placeholder={ph} inputMode={key === "sku" ? undefined : "numeric"} onChange={(e) => setPack(i, key, e.target.value)} className={`${input} ${errors[`packs.${i}.${key}`] ? "border-berry" : ""}`} />{err(`packs.${i}.${key}`)}</label>
              ))}
              <button type="button" aria-label="Remove pack" onClick={() => set("packs", form.packs.filter((_, n) => n !== i))} className="self-end justify-self-end rounded-xl p-2.5 text-berry hover:bg-berry/10"><Trash2 size={17} /></button>
            </div>
          ))}
        </div>
        <p className="mt-3 text-xs text-muted">The amount and unit give the price per kg, litre or piece shown to shoppers. An old price shows the pack as a deal.</p>
      </Card>

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-white/95 backdrop-blur lg:left-60">
        <div className="flex items-center justify-end gap-2 px-4 py-3 md:px-8">
          {Object.values(errors).some(Boolean) && <span className="mr-auto text-sm font-semibold text-berry">Please check the highlighted fields.</span>}
          <Link href="/admin/products" className={btn.ghost}>Cancel</Link>
          <button type="button" disabled={busy} onClick={save} className={btn.primary}><Save size={16} /> {busy ? "Saving…" : id ? "Save changes" : "Create product"}</button>
        </div>
      </div>
    </div>
  );
}
