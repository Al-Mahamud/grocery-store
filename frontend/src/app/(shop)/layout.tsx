import { Header } from "@/components/shop/Header";
import { Footer } from "@/components/shop/Footer";
import { AccountProvider } from "@/components/account/AccountProvider";
import { CartProvider } from "@/components/cart/CartProvider";
import { CartDrawer } from "@/components/cart/CartDrawer";

export default function ShopLayout({ children }: { children: React.ReactNode }) {
  return (
    <AccountProvider>
      <CartProvider>
        <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-xl focus:bg-ink focus:px-4 focus:py-2 focus:text-white">Skip to content</a>
        <Header />
        <main id="main">{children}</main>
        <Footer />
        <CartDrawer />
      </CartProvider>
    </AccountProvider>
  );
}
