"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { Cart } from "@/lib/types";
import { ApiError, request } from "@/lib/client";

type CartContext = {
  cart: Cart | null;
  open: boolean;
  setOpen: (open: boolean) => void;
  /** Quantity shown for a pack, including changes still on their way to the server. */
  quantityOf: (packId: number) => number;
  setQuantity: (packId: number, quantity: number) => void;
  applyCoupon: (code: string) => Promise<void>;
  removeCoupon: () => Promise<void>;
  refresh: () => Promise<void>;
  /** Shows a short message at the bottom of the screen. */
  notify: (message: string) => void;
  toast: string | null;
};

const Context = createContext<CartContext | null>(null);

/**
 * The bag for the whole shop. Quantity changes show at once and are sent to the server in order;
 * if the server refuses one (stock, limits), the bag snaps back and a short message explains why.
 */
export function CartProvider({ children }: { children: React.ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState<Record<number, number>>({});
  const [toast, setToast] = useState<string | null>(null);
  const queue = useRef<Promise<unknown>>(Promise.resolve());
  const toastTimer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const say = useCallback((message: string) => {
    setToast(message);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 3500);
  }, []);

  const refresh = useCallback(async () => {
    try {
      setCart(await request<Cart>("GET", "/cart"));
    } catch (e) {
      if (!(e instanceof ApiError)) throw e;
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- load the bag once on start.
    void refresh();
  }, [refresh]);

  const quantityOf = useCallback(
    (packId: number) => pending[packId] ?? cart?.items.find((i) => i.packId === packId)?.quantity ?? 0,
    [pending, cart],
  );

  const setQuantity = useCallback(
    (packId: number, quantity: number) => {
      setPending((p) => ({ ...p, [packId]: quantity }));
      queue.current = queue.current.then(async () => {
        try {
          setCart(await request<Cart>("PUT", `/cart/items/${packId}`, { quantity }));
        } catch (e) {
          say(e instanceof ApiError ? e.message : "Couldn't update your bag. Please try again.");
          await refresh();
        } finally {
          setPending((p) => {
            if (p[packId] !== quantity) return p;
            const next = { ...p };
            delete next[packId];
            return next;
          });
        }
      });
    },
    [refresh, say],
  );

  const value = useMemo<CartContext>(
    () => ({
      cart,
      open,
      setOpen,
      quantityOf,
      setQuantity,
      refresh,
      notify: say,
      toast,
      applyCoupon: async (code) => setCart(await request<Cart>("POST", "/cart/coupon", { code })),
      removeCoupon: async () => setCart(await request<Cart>("DELETE", "/cart/coupon")),
    }),
    [cart, open, quantityOf, setQuantity, refresh, say, toast],
  );

  return (
    <Context.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-6 z-[70] flex justify-center px-4">
        {toast && <p className="animate-rise rounded-full bg-ink px-5 py-3 text-sm font-medium text-white shadow-xl">{toast}</p>}
      </div>
    </Context.Provider>
  );
}

export function useCart(): CartContext {
  const context = useContext(Context);
  if (!context) throw new Error("useCart must be used inside CartProvider");
  return context;
}
