"use client";

import Link from "next/link";
import { User } from "lucide-react";
import { firstName, useAccount } from "./AccountProvider";

/** Header link: "Sign in" for guests, the customer's first name once signed in. */
export function AccountButton() {
  const { me } = useAccount();
  if (me) {
    return (
      <Link href="/account" className="hidden h-12 items-center gap-2 rounded-2xl px-2.5 hover:bg-cream sm:inline-flex" aria-label="Your account">
        <span className="grid h-8 w-8 place-items-center rounded-xl bg-leaf font-display text-sm font-bold text-white">{me.name.trim().charAt(0).toUpperCase()}</span>
        <span className="hidden text-left leading-tight lg:block">
          <span className="block text-[0.6875rem] text-muted">Hi, {firstName(me.name)}</span>
          <span className="block text-sm font-bold">Account</span>
        </span>
      </Link>
    );
  }
  return (
    <Link href="/login" className="hidden h-12 items-center gap-2 rounded-2xl px-2.5 hover:bg-cream sm:inline-flex" aria-label="Sign in">
      <User size={21} />
      <span className="hidden text-left leading-tight lg:block">
        <span className="block text-[0.6875rem] text-muted">Hello</span>
        <span className="block text-sm font-bold">Sign in</span>
      </span>
    </Link>
  );
}
