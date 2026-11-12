"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { BadgePercent, ClipboardList, ExternalLink, LayoutDashboard, LogOut, Menu, Package, Boxes, ShoppingBag, Truck, X } from "lucide-react";
import { request } from "@/lib/client";
import type { Staff } from "@/lib/admin-types";

type AdminContext = { staff: Staff; notify: (message: string) => void };
const Context = createContext<AdminContext | null>(null);

export function useAdmin(): AdminContext {
  const context = useContext(Context);
  if (!context) throw new Error("useAdmin must be used inside AdminShell");
  return context;
}

const nav = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/orders", label: "Orders", icon: ShoppingBag },
  { href: "/admin/picking", label: "Picking list", icon: ClipboardList },
  { href: "/admin/products", label: "Products", icon: Package },
  { href: "/admin/stock", label: "Stock", icon: Boxes },
  { href: "/admin/coupons", label: "Coupons", icon: BadgePercent },
  { href: "/admin/delivery", label: "Delivery", icon: Truck },
];

/** The admin frame: sidebar, staff name, sign-out, and a redirect to sign-in for everyone else. */
export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [staff, setStaff] = useState<Staff | null | undefined>(undefined);
  const [menu, setMenu] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const isLogin = pathname === "/admin/login";

  useEffect(() => {
    if (isLogin) return;
    request<{ staff: Staff | null }>("GET", "/staff/me")
      .then((r) => setStaff(r.staff))
      .catch(() => setStaff(null));
  }, [isLogin]);

  useEffect(() => {
    if (staff === null && !isLogin) router.replace(`/admin/login?next=${encodeURIComponent(pathname)}`);
  }, [staff, isLogin, pathname, router]);

  // eslint-disable-next-line react-hooks/set-state-in-effect -- close the phone menu after navigating.
  useEffect(() => setMenu(false), [pathname]);

  const notify = useCallback((message: string) => {
    setToast(message);
    setTimeout(() => setToast(null), 3500);
  }, []);

  if (isLogin) return <>{children}</>;
  if (!staff) return <div className="grid min-h-dvh place-items-center bg-cream text-sm text-muted" aria-busy="true">Loading the admin area…</div>;

  const active = (href: string) => (href === "/admin" ? pathname === "/admin" : pathname.startsWith(href));
  const links = (
    <ul className="space-y-1">
      {nav.map(({ href, label, icon: Icon }) => (
        <li key={href}>
          <Link href={href} aria-current={active(href) ? "page" : undefined} className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition ${active(href) ? "bg-white text-ink" : "text-white/75 hover:bg-white/10 hover:text-white"}`}>
            <Icon size={18} className={active(href) ? "text-tangerine" : ""} /> {label}
          </Link>
        </li>
      ))}
    </ul>
  );
  const footer = (
    <div className="space-y-2 border-t border-white/10 pt-4">
      <Link href="/" target="_blank" className="flex items-center gap-2 px-3 text-xs font-semibold text-white/60 hover:text-white"><ExternalLink size={14} /> Open the shop</Link>
      <div className="flex items-center gap-3 rounded-xl bg-white/5 p-3">
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-tangerine font-display font-bold">{staff.name.charAt(0)}</span>
        <span className="min-w-0 flex-1"><span className="block truncate text-sm font-bold">{staff.name}</span><span className="block text-xs text-white/55">{staff.phone}</span></span>
        <button
          type="button"
          aria-label="Sign out"
          onClick={async () => {
            await request("POST", "/staff/logout").catch(() => undefined);
            router.replace("/admin/login");
          }}
          className="grid h-9 w-9 place-items-center rounded-lg text-white/70 hover:bg-white/10 hover:text-white"
        >
          <LogOut size={17} />
        </button>
      </div>
    </div>
  );

  return (
    <Context.Provider value={{ staff, notify }}>
      <div className="min-h-dvh bg-[#f3f1ec] lg:grid lg:grid-cols-[15rem_1fr]">
        <aside className="sticky top-0 hidden h-dvh flex-col gap-6 bg-ink p-4 text-white lg:flex print:hidden">
          <Brand />
          <nav aria-label="Admin" className="flex-1">{links}</nav>
          {footer}
        </aside>
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between bg-ink px-4 text-white lg:hidden print:hidden">
          <Brand />
          <button type="button" aria-label="Menu" onClick={() => setMenu(true)} className="grid h-10 w-10 place-items-center rounded-xl bg-white/10"><Menu size={20} /></button>
        </header>
        {menu && (
          <div className="fixed inset-0 z-50 bg-ink/50 lg:hidden" onClick={() => setMenu(false)}>
            <div className="flex h-full w-72 flex-col gap-6 bg-ink p-4 text-white" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between"><Brand /><button type="button" aria-label="Close" onClick={() => setMenu(false)} className="grid h-9 w-9 place-items-center rounded-lg bg-white/10"><X size={18} /></button></div>
              <nav aria-label="Admin" className="flex-1">{links}</nav>
              {footer}
            </div>
          </div>
        )}
        <main className="min-w-0 px-4 py-6 md:px-8 md:py-8">{children}</main>
      </div>
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-6 z-[70] flex justify-center px-4 print:hidden">
        {toast && <p className="animate-rise rounded-full bg-ink px-5 py-3 text-sm font-medium text-white shadow-xl">{toast}</p>}
      </div>
    </Context.Provider>
  );
}

function Brand() {
  return (
    <Link href="/admin" className="flex items-center gap-2">
      <span className="grid h-8 w-8 place-items-center rounded-lg bg-tangerine font-display text-lg font-extrabold text-white">h</span>
      <span className="font-display text-lg font-extrabold">haat<span className="text-tangerine">.</span> <span className="text-xs font-bold uppercase tracking-widest text-sun">admin</span></span>
    </Link>
  );
}
