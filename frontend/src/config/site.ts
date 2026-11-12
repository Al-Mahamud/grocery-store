/** Brand settings in one place. Fees here are only for copy; the backend decides the real ones. */
export const site = {
  /** The public address of the shop, for links in search engines and shared posts. */
  url: (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, ""),
  name: "Haat",
  localName: "হাট",
  tagline: "Groceries delivered in your slot",
  description: "Fresh vegetables, fish, staples and household goods delivered across Dhaka in a two-hour slot you choose. Cash on delivery.",
  phone: "+880 1700-000000",
  email: "hello@haat.example",
  address: "Tejgaon I/A, Dhaka",
  deliveryFee: 49,
  freeDeliveryOver: 1500,
  minimumOrder: 300,
} as const;
