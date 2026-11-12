import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MapPin } from "lucide-react";
import { pages } from "@/config/pages";
import { api } from "@/lib/api";

type Props = { params: Promise<{ slug: string }> };

export function generateStaticParams() {
  return pages.map((p) => ({ slug: p.slug }));
}

export const dynamicParams = false;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const page = pages.find((p) => p.slug === slug);
  if (!page) return {};
  return { title: page.title, description: page.description, alternates: { canonical: `/pages/${page.slug}` } };
}

export default async function InfoPage({ params }: Props) {
  const { slug } = await params;
  const page = pages.find((p) => p.slug === slug);
  if (!page) notFound();
  const areas = slug === "delivery" ? await api.areas().catch(() => []) : [];
  return (
    <div className="container-page grid gap-8 py-8 md:grid-cols-[14rem_1fr] md:py-14">
      <nav aria-label="Information" className="order-2 md:order-1">
        <ul className="flex flex-wrap gap-1.5 md:flex-col">
          {pages.map((p) => (
            <li key={p.slug}>
              <Link href={`/pages/${p.slug}`} aria-current={p.slug === slug ? "page" : undefined} className={`block rounded-xl px-3 py-2 text-sm font-semibold ${p.slug === slug ? "bg-ink text-white" : "bg-white text-ink-soft hover:text-ink"}`}>{p.title}</Link>
            </li>
          ))}
        </ul>
      </nav>
      <article className="order-1 max-w-3xl md:order-2">
        <h1 className="font-display text-4xl font-extrabold tracking-tight md:text-5xl">{page.title}</h1>
        <p className="mt-4 text-lg text-ink-soft">{page.intro}</p>
        {areas.length > 0 && (
          <section className="mt-8 rounded-3xl bg-white p-5 md:p-6">
            <h2 className="font-display text-xl font-bold">Areas we deliver to</h2>
            <ul className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
              {areas.map((a) => <li key={a.id} className="flex items-center gap-1.5 text-sm font-semibold"><MapPin size={14} className="text-tangerine" /> {a.name}</li>)}
            </ul>
            <p className="mt-3 text-xs text-muted">Not on the list? We&rsquo;re adding areas — tell us where you are.</p>
          </section>
        )}
        {page.sections.map((s) => (
          <section key={s.heading} className="mt-8">
            <h2 className="font-display text-2xl font-bold">{s.heading}</h2>
            {s.body.map((para, i) => <p key={i} className="mt-2 leading-relaxed text-ink-soft">{para}</p>)}
          </section>
        ))}
      </article>
    </div>
  );
}
