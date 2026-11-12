import Link from "next/link";
import { ArrowLeft, Lock } from "lucide-react";
import { AccountProvider } from "@/components/account/AccountProvider";
import { CartProvider } from "@/components/cart/CartProvider";
import { Logo } from "@/components/shop/Logo";

export default function CheckoutLayout({ children }: { children: React.ReactNode }) {
  return (
    <AccountProvider>
      <CartProvider>
        <header className="border-b border-line bg-white">
          <div className="container-page flex h-16 items-center justify-between">
            <Link href="/cart" className="inline-flex items-center gap-1.5 rounded-xl bg-cream px-3 py-2 text-sm font-bold"><ArrowLeft size={15} /> Bag</Link>
            <Logo />
            <p className="flex items-center gap-1.5 text-xs font-semibold text-muted"><Lock size={13} /><span className="hidden sm:inline">Secure checkout</span></p>
          </div>
        </header>
        <main id="main">{children}</main>
      </CartProvider>
    </AccountProvider>
  );
}
