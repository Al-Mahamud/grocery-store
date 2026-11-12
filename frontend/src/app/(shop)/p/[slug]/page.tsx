import type { Metadata } from "next";
import { api } from "@/lib/api";
import { nextSlot } from "@/lib/grocery";
import { ProductView } from "@/components/product/ProductView";
import { ProductRail } from "@/components/shop/ProductRail";
import { SectionHeading } from "@/components/shop/SectionHeading";
import { site } from "@/config/site";

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await api.product((await params).slug);
  return {
    title: product.localName ? `${product.name} (${product.localName})` : product.name,
    description: product.description.slice(0, 160),
    alternates: { canonical: `/p/${product.slug}` },
    openGraph: { images: product.image ? [product.image] : [] },
  };
}

export default async function ProductPage({ params }: Props) {
  const { slug } = await params;
  const [product, related, days] = await Promise.all([api.product(slug), api.related(slug), api.slots()]);
  const next = nextSlot(days);
  const prices = product.packs.map((p) => p.price);
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    image: product.image ? [new URL(product.image, site.url).toString()] : [],
    url: `${site.url}/p/${product.slug}`,
    description: product.description,
    brand: product.brand ? { "@type": "Brand", name: product.brand } : { "@type": "Brand", name: site.name },
    offers: { "@type": "AggregateOffer", priceCurrency: "BDT", lowPrice: Math.min(...prices), highPrice: Math.max(...prices),
      availability: product.packs.some((p) => p.stock > 0) ? "https://schema.org/InStock" : "https://schema.org/OutOfStock" },
  };
  const breadcrumb = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: site.name, item: site.url },
      ...product.breadcrumb.map((c, i) => ({ "@type": "ListItem", position: i + 2, name: c.name, item: `${site.url}/c/${c.slug}` })),
      { "@type": "ListItem", position: product.breadcrumb.length + 2, name: product.name },
    ],
  };
  return (
    <div className="container-page space-y-12 pt-5">
      <ProductView product={product} nextSlot={next ? `${next.day}, ${next.label}` : null} />
      {related.length > 0 && (
        <section>
          <SectionHeading title="You might also need" />
          <ProductRail products={related} />
        </section>
      )}
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify([jsonLd, breadcrumb]).replace(/</g, "\\u003c") }} />
    </div>
  );
}
