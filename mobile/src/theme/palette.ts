/**
 * Non-Tailwind color tokens. NativeWind handles component styling via classes,
 * but charts, the navigation theme, and vector icons need raw hex at runtime.
 * These two palettes are kept deliberately close to the Tailwind tokens in
 * tailwind.config.js so the whole app reads as one system in both schemes.
 *
 * The categorical palette follows the dataviz guidance: distinct hues, similar
 * perceived weight, and legible on both light and dark surfaces.
 */

export interface Palette {
  scheme: 'light' | 'dark';
  background: string;
  card: string;
  cardAlt: string;
  border: string;
  text: string;
  textMuted: string;
  brand: string;
  positive: string;
  warning: string;
  danger: string;
  tabBar: string;
  tabActive: string;
  tabInactive: string;
  /** Distinct series colors for pie/bar/line charts. */
  categorical: string[];
}

const CATEGORICAL = [
  '#6366F1', // indigo
  '#10B981', // emerald
  '#F59E0B', // amber
  '#EF4444', // red
  '#3B82F6', // blue
  '#8B5CF6', // violet
  '#EC4899', // pink
  '#14B8A6', // teal
  '#F97316', // orange
  '#84CC16', // lime
];

export const lightPalette: Palette = {
  scheme: 'light',
  background: '#F3F4F6',
  card: '#FFFFFF',
  cardAlt: '#F9FAFB',
  border: '#E5E7EB',
  text: '#111827',
  textMuted: '#6B7280',
  brand: '#4F46E5',
  positive: '#16A34A',
  warning: '#D97706',
  danger: '#DC2626',
  tabBar: '#FFFFFF',
  tabActive: '#4F46E5',
  tabInactive: '#9CA3AF',
  categorical: CATEGORICAL,
};

export const darkPalette: Palette = {
  scheme: 'dark',
  background: '#0B1220',
  card: '#141C2B',
  cardAlt: '#1B2436',
  border: '#26334A',
  text: '#F3F4F6',
  textMuted: '#94A3B8',
  brand: '#818CF8',
  positive: '#4ADE80',
  warning: '#FBBF24',
  danger: '#F87171',
  tabBar: '#0F1727',
  tabActive: '#818CF8',
  tabInactive: '#64748B',
  categorical: CATEGORICAL,
};

export function paletteFor(scheme: 'light' | 'dark'): Palette {
  return scheme === 'dark' ? darkPalette : lightPalette;
}
