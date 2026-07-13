import { DarkTheme, DefaultTheme, type Theme } from '@react-navigation/native';

import type { Palette } from '@/theme/palette';

/**
 * Build a React Navigation theme from our runtime palette so the navigation
 * chrome (backgrounds, headers, card transitions) matches the rest of the app
 * in both light and dark mode.
 */
export function navThemeFor(palette: Palette): Theme {
  const base = palette.scheme === 'dark' ? DarkTheme : DefaultTheme;
  return {
    ...base,
    colors: {
      ...base.colors,
      primary: palette.brand,
      background: palette.background,
      card: palette.card,
      text: palette.text,
      border: palette.border,
      notification: palette.danger,
    },
  };
}
