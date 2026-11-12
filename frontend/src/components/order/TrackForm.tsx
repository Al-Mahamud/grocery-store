"use client";

import { useState } from "react";
import { ApiError, request } from "@/lib/client";
import type { OrderView } from "@/lib/types";
import { OrderDetails, StatusTimeline } from "./OrderDetails";

export function TrackForm({ initialNumber }: { initialNumber: string }) {
  const [number, setNumber] = useState(initialNumber);
  const [phone, setPhone] = useState("");
  const [order, setOrder] = useState<OrderView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  return (
    <>
      <form
        className="mt-6 grid gap-2 rounded-3xl bg-white p-3 sm:grid-cols-[1fr_1fr_auto]"
        onSubmit={async (e) => {
          e.preventDefault();
          setLoading(true);
          setError(null);
          try {
            setOrder(await request<OrderView>("GET", `/orders/track?number=${encodeURIComponent(number)}&phone=${encodeURIComponent(phone)}`));
          } catch (err) {
            setOrder(null);
            setError(err instanceof ApiError && err.status === 404 ? "No order matches those details. Check both numbers." : "Something went wrong. Please try again.");
          } finally {
            setLoading(false);
          }
        }}
      >
        <label><span className="sr-only">Order number</span><input required value={number} onChange={(e) => setNumber(e.target.value.toUpperCase())} placeholder="Order number, e.g. HT10001" className="w-full rounded-xl bg-cream px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-tangerine" /></label>
        <label><span className="sr-only">Mobile number</span><input required type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Mobile number" className="w-full rounded-xl bg-cream px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-tangerine" /></label>
        <button type="submit" disabled={loading} className="rounded-xl bg-tangerine px-7 py-3 text-sm font-bold text-white disabled:opacity-50">{loading ? "Looking…" : "Track"}</button>
      </form>
      {error && <p role="alert" className="mt-3 text-sm font-semibold text-berry">{error}</p>}
      {order && (
        <div className="mt-6 animate-rise space-y-4">
          <div className="rounded-3xl bg-white p-5 md:p-6"><StatusTimeline order={order} /></div>
          <OrderDetails order={order} />
        </div>
      )}
    </>
  );
}
