"use client";

import { useEffect, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";

const noop = () => () => {};

/**
 * A panel that slides in from the side, with a dimmed backdrop. Escape and backdrop clicks close it.
 * Rendered into <body>, so a parent with a blur or transform (like the sticky header) can't clip it.
 */
export function Drawer({
  open,
  onClose,
  side = "right",
  label,
  children,
}: {
  open: boolean;
  onClose: () => void;
  side?: "left" | "right";
  label: string;
  children: React.ReactNode;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = overflow;
      window.removeEventListener("keydown", onKey);
    };
  }, [open, onClose]);
  const inBrowser = useSyncExternalStore(noop, () => true, () => false);
  if (!inBrowser) return null;

  return createPortal(
    <div
      className={`fixed inset-0 z-50 ${open ? "visible" : "pointer-events-none invisible"}`}
      style={{ transition: `visibility 0s linear ${open ? 0 : 300}ms` }}
      aria-hidden={!open}
    >
      <div
        className={`absolute inset-0 bg-ink/40 transition-opacity duration-300 ${open ? "opacity-100" : "opacity-0"}`}
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal
        aria-label={label}
        className={`absolute inset-y-2 ${side === "left" ? "left-2" : "right-2"} flex w-[min(27rem,calc(100vw-1rem))] flex-col overflow-hidden rounded-3xl bg-white shadow-2xl transition-transform duration-300 ease-out-soft ${
          open ? "translate-x-0" : side === "left" ? "-translate-x-[calc(100%+1rem)]" : "translate-x-[calc(100%+1rem)]"
        }`}
      >
        {open && children}
      </div>
    </div>,
    document.body,
  );
}
