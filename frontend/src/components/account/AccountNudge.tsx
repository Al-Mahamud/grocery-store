"use client";

import Link from "next/link";
import { RotateCcw } from "lucide-react";
import { useAccount } from "./AccountProvider";

/** After a guest order: suggest an account so the next shop is quicker. */
export function AccountNudge() {
  const { me } = useAccount();
  if (me !== null) return null;
  return (
    <div className="flex flex-wrap items-center gap-4 rounded-3xl bg-ink p-5 text-white md:p-6">
      <span className="grid h-12 w-12 place-items-center rounded-2xl bg-white/10 text-sun"><RotateCcw size={22} /></span>
      <div className="min-w-0 flex-1">
        <p className="font-display text-lg font-bold">Make the next shop a one-minute job</p>
        <p className="text-sm text-white/70">With an account you can repeat this order, keep your address and see every order in one place.</p>
      </div>
      <Link href="/register?next=/account" className="rounded-xl bg-sun px-5 py-3 text-sm font-bold text-ink hover:bg-white">Create an account</Link>
    </div>
  );
}
