import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CalendarClock } from "lucide-react";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { slotText } from "@/lib/slots";
import { OrderDetails, StatusTimeline } from "@/components/order/OrderDetails";
import { AccountNudge } from "@/components/account/AccountNudge";

export const metadata: Metadata = { title: "Order placed", robots: { index: false } };

export default async function OrderPage({ params, searchParams }: { params: Promise<{ number: string }>; searchParams: Promise<SearchParams> }) {
  const { number } = await params;
  const token = first((await searchParams).t);
  if (!token) notFound();
  const order = await api.order(number, token);
  return (
    <div className="container-page max-w-5xl space-y-4 py-6 md:py-10">
      <div className="animate-rise overflow-hidden rounded-3xl bg-leaf p-6 text-white md:p-10">
        <p className="text-sm font-bold text-white/80">Order {order.number}</p>
        <h1 className="mt-2 font-display text-4xl font-extrabold tracking-tight md:text-5xl">Thanks, {order.name.split(" ")[0]}! Your groceries are booked.</h1>
        <p className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-leaf"><CalendarClock size={20} /> {slotText(order.slot)}</p>
        <p className="mt-4 max-w-lg text-sm text-white/85">We&rsquo;ll text {order.phone} when your order is packed and again when the rider is on the way.</p>
      </div>
      <div className="rounded-3xl bg-white p-5 md:p-6"><StatusTimeline order={order} /></div>
      <OrderDetails order={order} />
      <AccountNudge />
      <div className="flex flex-wrap justify-center gap-2 pt-4">
        <Link href="/" className="rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Continue shopping</Link>
        <Link href={`/track?number=${order.number}`} className="rounded-xl border-2 border-ink px-6 py-3 text-sm font-bold">Track order</Link>
      </div>
    </div>
  );
}
