"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, MapPin, Package, RotateCcw, UserRound } from "lucide-react";
import { useCart } from "@/components/cart/CartProvider";
import { firstName, useAccount } from "./AccountProvider";

const tabs = [
  { href: "/account", label: "Orders", icon: Package },
  { href: "/account/buy-again", label: "Buy again", icon: RotateCcw },
  { href: "/account/addresses", label: "Addresses", icon: MapPin },
  { href: "/account/profile", label: "Profile", icon: UserRound },
];

/** Frame for every account page: greeting, tabs, and a redirect to sign-in for guests. */
export function AccountShell({ children }: { children: React.ReactNode }) {
  const { me, signOut } = useAccount();
  const { refresh } = useCart();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (me === null) router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [me, pathname, router]);

  if (!me) {
    return (
      <div className="container-page py-10" aria-busy="true">
        <div className="h-24 animate-pulse rounded-3xl bg-white" />
        <div className="mt-4 h-72 animate-pulse rounded-3xl bg-white" />
      </div>
    );
  }

  const active = (href: string) => (href === "/account" ? pathname === "/account" || pathname.startsWith("/account/orders") : pathname.startsWith(href));

  return (
    <div className="container-page py-6 md:py-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex items-center gap-4">
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-leaf font-display text-2xl font-extrabold text-white">{me.name.trim().charAt(0).toUpperCase()}</span>
          <div>
            <h1 className="font-display text-3xl font-extrabold tracking-tight md:text-4xl">Hi, {firstName(me.name)}</h1>
            <p className="text-sm text-muted">{me.phone}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={async () => {
            await signOut();
            await refresh();
            router.replace("/");
          }}
          className="inline-flex items-center gap-2 rounded-xl border-2 border-line bg-white px-4 py-2.5 text-sm font-bold hover:border-ink"
        >
          <LogOut size={16} /> Sign out
        </button>
      </div>
      <div className="mt-6 grid grid-cols-1 gap-5 lg:grid-cols-[14rem_minmax(0,1fr)]">
        <nav aria-label="Account" className="min-w-0">
          <ul className="no-scrollbar -mx-4 flex gap-1.5 overflow-x-auto px-4 lg:mx-0 lg:flex-col lg:px-0">
            {tabs.map(({ href, label, icon: Icon }) => (
              <li key={href}>
                <Link
                  href={href}
                  aria-current={active(href) ? "page" : undefined}
                  className={`flex items-center gap-2.5 whitespace-nowrap rounded-xl px-4 py-3 text-sm font-bold transition ${active(href) ? "bg-ink text-white" : "bg-white text-ink-soft hover:text-ink"}`}
                >
                  <Icon size={17} className={active(href) ? "text-sun" : ""} /> {label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <div className="min-w-0">{children}</div>
      </div>
    </div>
  );
}
