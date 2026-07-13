import type { ComponentProps } from 'react';
import { MaterialCommunityIcons } from '@expo/vector-icons';

type MCIName = ComponentProps<typeof MaterialCommunityIcons>['name'];

/**
 * The backend stores FontAwesome-style icon slugs on categories (see
 * DefaultCategories.java: "utensils", "chart-pie", …). The app renders with
 * @expo/vector-icons' MaterialCommunityIcons, so we translate the slugs here.
 * Unknown/user-invented slugs fall back to a neutral tag icon.
 */
const ICON_MAP: Record<string, MCIName> = {
  // expense defaults
  utensils: 'silverware-fork-knife',
  plane: 'airplane',
  bolt: 'flash',
  'heart-pulse': 'heart-pulse',
  film: 'film',
  'shopping-bag': 'shopping',
  'shopping-cart': 'cart',
  ellipsis: 'dots-horizontal',
  // investment defaults
  landmark: 'bank',
  'chart-pie': 'chart-pie',
  'chart-line': 'chart-line',
  'piggy-bank': 'piggy-bank',
  shield: 'shield',
  umbrella: 'umbrella',
  coins: 'cash-multiple',
  bitcoin: 'bitcoin',
  // a few extras a user might pick
  home: 'home',
  car: 'car',
  gift: 'gift',
  heart: 'heart',
  book: 'book',
  wifi: 'wifi',
  phone: 'cellphone',
  gold: 'gold',
};

export function resolveCategoryIcon(slug: string | null | undefined): MCIName {
  if (!slug) return 'tag-outline';
  return ICON_MAP[slug] ?? 'tag-outline';
}

/** The slugs offered in the category editor's icon picker. */
export const ICON_CHOICES: string[] = [
  'utensils', 'plane', 'bolt', 'heart-pulse', 'film', 'shopping-bag', 'shopping-cart',
  'ellipsis', 'home', 'car', 'gift', 'book', 'phone', 'wifi',
  'landmark', 'chart-pie', 'chart-line', 'piggy-bank', 'shield', 'umbrella', 'coins',
  'bitcoin', 'gold',
];
