import { ImageResponse } from "next/og";
import { site } from "@/config/site";

export const alt = `${site.name} — ${site.tagline}`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/** The heading font as TrueType (what the image renderer reads); null falls back to the default font. */
async function headingFont(): Promise<ArrayBuffer | null> {
  try {
    const css = await (await fetch("https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@800")).text();
    const url = css.match(/src: url\((.+?)\) format\('(?:truetype|opentype)'\)/)?.[1];
    return url ? await (await fetch(url)).arrayBuffer() : null;
  } catch {
    return null;
  }
}

/** The picture shown when someone shares a Haat link (product pages use the product's own). */
export default async function OpenGraphImage() {
  const font = await headingFont();
  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between", background: "#17a05d", color: "white", padding: 72, fontFamily: font ? "Bricolage" : undefined }}>
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div style={{ width: 84, height: 84, borderRadius: 24, background: "#ff6b35", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 60, fontWeight: 800 }}>h</div>
          <div style={{ display: "flex", fontSize: 64, fontWeight: 800 }}>haat<span style={{ color: "#ffd23f" }}>.</span></div>
        </div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ fontSize: 76, fontWeight: 800, lineHeight: 1.02, maxWidth: 900 }}>Fresh groceries, in the slot you choose.</div>
          <div style={{ marginTop: 28, fontSize: 32, opacity: 0.9 }}>{`Delivered across Dhaka · Free over Tk ${site.freeDeliveryOver.toLocaleString("en-IN")} · Cash on delivery`}</div>
        </div>
      </div>
    ),
    { ...size, fonts: font ? [{ name: "Bricolage", data: font, weight: 800, style: "normal" }] : undefined },
  );
}
