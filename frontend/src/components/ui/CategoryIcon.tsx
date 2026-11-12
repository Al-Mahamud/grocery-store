import { Apple, Bath, Bean, Carrot, Coffee, Cookie, Croissant, CupSoda, Droplet, Egg, Fish, Flame, Leaf, Milk, ShoppingBasket, Sparkles, SprayCan, Wheat } from "lucide-react";

const icons = {
  leaf: Leaf, carrot: Carrot, apple: Apple, fish: Fish, wheat: Wheat, droplet: Droplet, bean: Bean, flame: Flame,
  milk: Milk, egg: Egg, croissant: Croissant, cookie: Cookie, coffee: Coffee, "cup-soda": CupSoda,
  sparkles: Sparkles, "spray-can": SprayCan, bath: Bath, basket: ShoppingBasket,
} as const;

/** Category icons by the name stored in the database; unknown names fall back to a basket. */
export function CategoryIcon({ name, size = 20 }: { name: string; size?: number }) {
  const Icon = icons[name as keyof typeof icons] ?? ShoppingBasket;
  return <Icon size={size} strokeWidth={1.9} aria-hidden />;
}
