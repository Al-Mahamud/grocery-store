/** "2026-11-12" + "18:00:00" → "Thu 12 Nov, 6–8 pm" for order pages. */
export function slotText(slot: { date: string; start: string; end: string }): string {
  const date = new Date(`${slot.date}T00:00:00`);
  const day = date.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
  const hour = (t: string) => {
    const h = Number(t.slice(0, 2));
    return { n: h % 12 === 0 ? 12 : h % 12, pm: h >= 12 };
  };
  const a = hour(slot.start);
  const b = hour(slot.end);
  const range = a.pm === b.pm ? `${a.n}–${b.n} ${b.pm ? "pm" : "am"}` : `${a.n} ${a.pm ? "pm" : "am"}–${b.n} ${b.pm ? "pm" : "am"}`;
  return `${day}, ${range}`;
}
