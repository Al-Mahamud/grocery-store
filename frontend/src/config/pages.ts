import { site } from "./site";

/**
 * The information pages linked from the footer. Written for a shop that is not open yet: the owner
 * should check every promise here (and have Terms and Privacy read by a lawyer) before launch.
 */
export type InfoPage = {
  slug: string;
  title: string;
  description: string;
  intro: string;
  sections: { heading: string; body: string[] }[];
};

const fee = `৳${site.deliveryFee}`;
const freeOver = `৳${site.freeDeliveryOver.toLocaleString("en-IN")}`;

export const pages: InfoPage[] = [
  {
    slug: "about",
    title: "About Haat",
    description: "Haat is an online grocery shop in Dhaka that delivers in a two-hour slot you choose.",
    intro: "A haat is the weekly village market: fresh produce, people you know, fair prices. We want ordering groceries in Dhaka to feel the same, without the traffic.",
    sections: [
      { heading: "What we do", body: ["We stock vegetables, fruit, fish and meat, rice, oil, dal and spices, dairy, snacks and household essentials, and deliver them across Dhaka in two-hour slots from 8 am to 10 pm.", "Fresh produce arrives at our hub every morning. Every pack shows its price per kg, litre or piece, so you can compare fairly."] },
      { heading: "How we work", body: ["You choose the slot; we keep slots small so riders arrive on time.", "If something runs out while we pack, we do what you told us at checkout: call you, replace it with something similar at the same or lower price, or take it off the bill.", "Pay the rider in cash when you've checked your bag, or pay online with bKash, Nagad or a card."] },
    ],
  },
  {
    slug: "delivery",
    title: "Delivery areas & slots",
    description: `Where Haat delivers in Dhaka, the two-hour delivery slots, and delivery fees (${fee}, free over ${freeOver}).`,
    intro: "We deliver to the Dhaka areas listed below, every day from 8 am to 10 pm, in two-hour slots.",
    sections: [
      { heading: "Slots", body: ["Choose a two-hour slot at checkout for today or the next two days: 8–10 am, 10 am–12 pm, 12–2 pm, 2–4 pm, 4–6 pm, 6–8 pm or 8–10 pm.", "A slot closes two hours before it starts, or earlier when it is full. Full slots aren't offered."] },
      { heading: "Fees", body: [`Delivery costs ${fee}. It's free when your order is ${freeOver} or more after discounts.`, `The minimum order is ৳${site.minimumOrder}.`] },
      { heading: "On the day", body: ["We text you when your order is on its way. The rider calls when they arrive.", "Fish, meat and some vegetables are sold by approximate weight, so the final weight can differ slightly; you pay for what you get."] },
    ],
  },
  {
    slug: "returns",
    title: "Returns & refunds",
    description: "How to return groceries to Haat and how refunds work for cash and online payments.",
    intro: "Check your bag when the rider arrives. If something isn't right, tell us — we'll make it right.",
    sections: [
      { heading: "At the door", body: ["You can hand back any item before paying, and you won't be charged for it.", "Damaged or wrong items: show the rider, and we'll take them off the bill or bring a replacement in your next slot."] },
      { heading: "After delivery", body: ["Fresh food (vegetables, fruit, fish, meat, dairy, bread): tell us within 24 hours with a photo.", "Packed food and household goods: within 7 days, unopened and with the seal intact."] },
      { heading: "Refunds", body: ["Cash orders are refunded to your bKash or Nagad number, or taken off your next order.", "Online payments go back to the same card or wallet within 7 working days. If an online order is cancelled after you paid, we refund it in full automatically."] },
    ],
  },
  {
    slug: "contact",
    title: "Contact us",
    description: "Call, email or visit Haat, the Dhaka grocery delivery shop.",
    intro: "We answer every day from 8 am to 10 pm.",
    sections: [
      { heading: "Call or text", body: [`${site.phone} — the quickest way, especially about today's delivery.`] },
      { heading: "Email", body: [`${site.email} — for refunds, business orders and anything that can wait a day.`] },
      { heading: "Visit", body: [`${site.address}. Our hub packs orders; there's no walk-in shop.`] },
      { heading: "About an order", body: ["Have your order number ready (it starts with HT). You can also follow it on the Track your order page."] },
    ],
  },
  {
    slug: "privacy",
    title: "Privacy",
    description: "What Haat collects, why, and what you can ask us to do with it.",
    intro: "We collect only what we need to deliver your groceries, and we don't sell it.",
    sections: [
      { heading: "What we keep", body: ["Your name, mobile number, delivery addresses and, if you give it, your email.", "What you ordered, when, and how you paid. We never see or keep card numbers or wallet PINs; payments are handled by SSLCommerz."] },
      { heading: "Why", body: ["To deliver your order, text you about it, show your order history and \"buy again\", and keep the accounts that tax law requires."] },
      { heading: "Who else sees it", body: ["Our riders see your name, number and address for the delivery. Our SMS provider receives your number and the text. SSLCommerz handles online payments. Nobody else, unless the law requires it."] },
      { heading: "Cookies", body: ["We use three: one keeps you signed in, one remembers your bag, and one protects forms from forgery. No advertising or tracking cookies."] },
      { heading: "Your choices", body: [`Ask us at ${site.email} to see, correct or delete your data. We delete accounts on request but keep order records for as long as the law requires.`] },
    ],
  },
  {
    slug: "terms",
    title: "Terms of service",
    description: "The terms for ordering groceries from Haat.",
    intro: "By placing an order you agree to these terms.",
    sections: [
      { heading: "Orders and prices", body: ["Prices are in taka and include VAT where it applies. The price you see at checkout is the price you pay, except for items sold by approximate weight, which are charged for the weight delivered.", "An order is confirmed when you get the confirmation text. Online orders are confirmed once payment arrives; unpaid online orders are cancelled after 30 minutes."] },
      { heading: "Substitutions", body: ["If an item runs out, we follow the choice you made at checkout. Replacements are similar items at the same or a lower price."] },
      { heading: "Delivery", body: ["We aim to arrive within your slot. If we can't, we'll call you. Someone needs to be available to receive the order."] },
      { heading: "Cancelling", body: ["You can cancel by calling us until the order is packed. We may cancel an order if items are unavailable, the address can't be reached, or we suspect misuse; any payment is refunded in full."] },
      { heading: "Accounts", body: ["Keep your password private. We'll never ask for your password or a code we text you."] },
    ],
  },
  {
    slug: "careers",
    title: "Careers",
    description: "Work at Haat: packing, riding and customer care in Dhaka.",
    intro: "We're a small team. When we hire, we look for packers, riders and customer-care people who care about getting the details right.",
    sections: [
      { heading: "Open roles", body: [`There are no open roles right now. Send your CV to ${site.email} with the role you'd like in the subject, and we'll keep it on file.`] },
    ],
  },
];

export const pageSlugs = pages.map((p) => p.slug);
