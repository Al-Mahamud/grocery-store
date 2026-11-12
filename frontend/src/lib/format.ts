/** "12 Nov, 4:35 pm" in Dhaka time. */
export function dateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-GB", { timeZone: "Asia/Dhaka", day: "numeric", month: "short", hour: "numeric", minute: "2-digit", hour12: true });
}

/** "Fri 13 Nov" for a yyyy-mm-dd date. */
export function day(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
}

/** "10:00:00" → "10 am". */
export function hour(time: string): string {
  const h = Number(time.slice(0, 2));
  return `${h % 12 === 0 ? 12 : h % 12} ${h >= 12 ? "pm" : "am"}`;
}
