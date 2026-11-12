import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CalendarClock, CircleCheck, XCircle } from "lucide-react";
import { api } from "@/lib/api";
import { first, type SearchParams } from "@/lib/listing";
import { slotText } from "@/lib/slots";
import { taka } from "@/lib/money";
import { OrderDetails, StatusTimeline } from "@/components/order/OrderDetails";
import { AccountNudge } from "@/components/account/AccountNudge";
import { PaymentPanel } from "@/components/payment/PaymentPanel";

export const metadata: Metadata = { title: "Your order", robots: { index: false } };

const outcomes = ["success", "fail", "cancel"] as const;

export default async function OrderPage({ params, searchParams }: { params: Promise<{ number: string }>; searchParams: Promise<SearchParams> }) {
  const { number } = await params;
  const query = await searchParams;
  const token = first(query.t);
  if (!token) notFound();
  const order = await api.order(number, token);
  const payment = outcomes.find((o) => o === first(query.payment));
  const firstName = order.name.split(" ")[0];
  const paid = order.paymentStatus === "PAID";

  return (
    <div className="container-page max-w-5xl space-y-4 py-6 md:py-10">
      {order.status === "PENDING_PAYMENT" ? (
        <PaymentPanel order={order} token={token} outcome={payment} />
      ) : order.status === "CANCELLED" ? (
        <div className="animate-rise rounded-3xl bg-white p-6 md:p-10">
          <XCircle className="text-berry" size={36} />
          <p className="mt-3 text-sm font-bold text-muted">Order {order.number}</p>
          <h1 className="mt-1 font-display text-4xl font-extrabold tracking-tight">This order was cancelled.</h1>
          <p className="mt-2 max-w-lg text-sm text-ink-soft">
            {paid
              ? `We received ${taka(order.total)} after the order closed. We'll refund it to the same account — no need to do anything.`
              : "It wasn't paid in time, so nothing was charged and the items went back on the shelf."}
          </p>
          <Link href="/cart" className="mt-5 inline-block rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Shop again</Link>
        </div>
      ) : (
        <div className="animate-rise overflow-hidden rounded-3xl bg-leaf p-6 text-white md:p-10">
          <p className="text-sm font-bold text-white/80">Order {order.number}</p>
          <h1 className="mt-2 font-display text-4xl font-extrabold tracking-tight md:text-5xl">Thanks, {firstName}! Your groceries are booked.</h1>
          <div className="mt-5 flex flex-wrap gap-2">
            <p className="inline-flex items-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-leaf"><CalendarClock size={20} /> {slotText(order.slot)}</p>
            {paid && <p className="inline-flex items-center gap-2 rounded-2xl bg-white/15 px-4 py-3 font-bold"><CircleCheck size={20} /> Paid {taka(order.total)}</p>}
          </div>
          <p className="mt-4 max-w-lg text-sm text-white/85">We&rsquo;ll text {order.phone} when your order is packed and again when the rider is on the way.</p>
        </div>
      )}
      {order.status !== "PENDING_PAYMENT" && order.status !== "CANCELLED" && (
        <div className="rounded-3xl bg-white p-5 md:p-6"><StatusTimeline order={order} /></div>
      )}
      <OrderDetails order={order} />
      {order.status !== "PENDING_PAYMENT" && <AccountNudge />}
      <div className="flex flex-wrap justify-center gap-2 pt-4">
        <Link href="/" className="rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Continue shopping</Link>
        <Link href={`/track?number=${order.number}`} className="rounded-xl border-2 border-ink px-6 py-3 text-sm font-bold">Track order</Link>
      </div>
    </div>
  );
}
