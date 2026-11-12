import type { Metadata, Viewport } from "next";
import { Bricolage_Grotesque, DM_Sans, Noto_Sans_Bengali } from "next/font/google";
import { site } from "@/config/site";
import "./globals.css";

const dm = DM_Sans({ subsets: ["latin"], variable: "--font-dm", display: "swap" });
const bricolage = Bricolage_Grotesque({ subsets: ["latin"], variable: "--font-bricolage", display: "swap" });
const bengali = Noto_Sans_Bengali({ subsets: ["bengali"], variable: "--font-bengali", display: "swap", weight: ["400", "500", "600", "700"] });

export const metadata: Metadata = {
  title: { default: `${site.name} — ${site.tagline}`, template: `%s · ${site.name}` },
  description: site.description,
};

export const viewport: Viewport = { themeColor: "#f7f4ee" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${dm.variable} ${bricolage.variable} ${bengali.variable}`}>
      <body className="min-h-dvh font-sans">{children}</body>
    </html>
  );
}
