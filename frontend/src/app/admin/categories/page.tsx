"use client";

import { useEffect, useState } from "react";
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { CategoryAdmin } from "@/lib/admin-types";
import { CategoryIcon } from "@/components/ui/CategoryIcon";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const ICONS = ["leaf", "carrot", "apple", "fish", "wheat", "droplet", "bean", "flame", "milk", "egg", "croissant", "cookie", "coffee", "cup-soda", "sparkles", "spray-can", "bath", "basket"];
const COLORS = ["#E5F4E3", "#FFF0D6", "#E2EEF9", "#FBEBD3", "#FFF4C2", "#FDE4D3", "#FDE0DC", "#E5F0FF", "#F8EEDF", "#F3E5F5", "#E0F4F7", "#E6F2EE", "#FCE8EF"];

type Editing = { id: number | null; parentId: number | null; name: string; icon: string; color: string };

export default function CategoriesPage() {
  const { notify } = useAdmin();
  const [tree, setTree] = useState<CategoryAdmin[] | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    request<CategoryAdmin[]>("GET", "/admin/categories").then(setTree);
  }, []);

  const call = async (method: string, path: string, body: unknown, done: string) => {
    try {
      setTree(await request<CategoryAdmin[]>(method, path, body));
      notify(done);
      return true;
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fields);
        if (!Object.keys(e.fields).length) notify(e.message);
      }
      return false;
    }
  };
  const edit = (c: CategoryAdmin | null, parentId: number | null) => {
    setErrors({});
    setEditing(c ? { id: c.id, parentId: c.parentId, name: c.name, icon: c.icon, color: c.color } : { id: null, parentId, name: "", icon: parentId ? "basket" : "leaf", color: "#E5F4E3" });
  };

  const row = (c: CategoryAdmin, top: boolean) => (
    <div className={`flex items-center gap-3 rounded-xl px-3 py-2 ${top ? "bg-cream/70" : ""}`}>
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg text-ink-soft" style={{ backgroundColor: c.color }}><CategoryIcon name={c.icon} size={18} /></span>
      <span className="min-w-0 flex-1">
        <span className={`block truncate ${top ? "font-bold" : "font-semibold"}`}>{c.name}</span>
        <span className="text-xs text-muted">/c/{c.slug} · {c.products} product{c.products === 1 ? "" : "s"}</span>
      </span>
      <span className="flex gap-0.5">
        <button type="button" aria-label={`Move ${c.name} up`} onClick={() => call("POST", `/admin/categories/${c.id}/move`, { direction: -1 }, "Moved")} className="rounded-lg p-2 text-ink-soft hover:bg-white"><ArrowUp size={15} /></button>
        <button type="button" aria-label={`Move ${c.name} down`} onClick={() => call("POST", `/admin/categories/${c.id}/move`, { direction: 1 }, "Moved")} className="rounded-lg p-2 text-ink-soft hover:bg-white"><ArrowDown size={15} /></button>
        <button type="button" aria-label={`Edit ${c.name}`} onClick={() => edit(c, c.parentId)} className="rounded-lg p-2 text-ink-soft hover:bg-white"><Pencil size={15} /></button>
        <button type="button" aria-label={`Delete ${c.name}`} onClick={() => window.confirm(`Delete “${c.name}”?`) && call("DELETE", `/admin/categories/${c.id}`, undefined, `${c.name} deleted`)} className="rounded-lg p-2 text-berry hover:bg-white"><Trash2 size={15} /></button>
      </span>
    </div>
  );

  return (
    <>
      <PageHeader title="Categories" note="Aisles and the shelves inside them, in the order shoppers see them.">
        <button type="button" onClick={() => edit(null, null)} className={btn.primary}><Plus size={16} /> New aisle</button>
      </PageHeader>
      {!tree ? <Loading /> : (
        <div className="grid gap-3 xl:grid-cols-2">
          {tree.map((top) => (
            <Card key={top.id}>
              {row(top, true)}
              <div className="mt-1 space-y-0.5 pl-4">
                {top.children.map((c) => <div key={c.id}>{row(c, false)}</div>)}
                <button type="button" onClick={() => edit(null, top.id)} className="mt-1 inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold text-tangerine hover:bg-cream"><Plus size={14} /> Add a shelf to {top.name}</button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <p className="mt-3 text-xs text-muted">A category&rsquo;s web address is fixed when it&rsquo;s made, so renaming never breaks links. Only empty categories can be deleted; archived products count.</p>

      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing?.id ? "Edit category" : editing?.parentId ? "New shelf" : "New aisle"}>
        {editing && (
          <div className="space-y-4">
            <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">Name</span>
              <input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} className={`${input} ${errors.name ? "border-berry" : ""}`} />
              {errors.name && <span className="mt-0.5 block text-xs font-semibold text-berry">{errors.name}</span>}</label>
            <fieldset>
              <legend className="mb-1 text-xs font-bold text-ink-soft">Icon</legend>
              <div className="flex flex-wrap gap-1.5">
                {ICONS.map((i) => (
                  <button key={i} type="button" aria-label={i} aria-pressed={editing.icon === i} onClick={() => setEditing({ ...editing, icon: i })} className={`grid h-10 w-10 place-items-center rounded-xl border-2 ${editing.icon === i ? "border-ink bg-cream" : "border-line"}`}><CategoryIcon name={i} size={18} /></button>
                ))}
              </div>
            </fieldset>
            <fieldset>
              <legend className="mb-1 text-xs font-bold text-ink-soft">Colour</legend>
              <div className="flex flex-wrap gap-1.5">
                {COLORS.map((c) => (
                  <button key={c} type="button" aria-label={c} aria-pressed={editing.color.toUpperCase() === c} onClick={() => setEditing({ ...editing, color: c })} className={`h-9 w-9 rounded-xl border-2 ${editing.color.toUpperCase() === c ? "border-ink" : "border-transparent"}`} style={{ backgroundColor: c }} />
                ))}
              </div>
            </fieldset>
            {errors.parentId && <p className="text-sm font-semibold text-berry">{errors.parentId}</p>}
            <button type="button" className={btn.primary} onClick={async () => {
              const ok = editing.id
                ? await call("PUT", `/admin/categories/${editing.id}`, { name: editing.name, icon: editing.icon, color: editing.color }, "Saved")
                : await call("POST", "/admin/categories", { name: editing.name, icon: editing.icon, color: editing.color, parentId: editing.parentId }, `${editing.name} added`);
              if (ok) setEditing(null);
            }}>Save</button>
          </div>
        )}
      </Modal>
    </>
  );
}
