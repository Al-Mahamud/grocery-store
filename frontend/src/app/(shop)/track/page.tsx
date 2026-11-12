import type { Metadata } from "next";
import { TrackForm } from "@/components/order/TrackForm";
import { first, type SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Track your order" };

export default async function TrackPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  return (
    <div className="container-page max-w-5xl py-6 md:py-10">
      <h1 className="font-display text-4xl font-extrabold tracking-tight">Track your order</h1>
      <p className="mt-1 text-sm text-muted">Enter your order number (starts with HT) and the mobile number you used.</p>
      <TrackForm initialNumber={first((await searchParams).number) ?? ""} />
    </div>
  );
}
