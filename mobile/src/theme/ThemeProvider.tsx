import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useColorScheme as useDeviceColorScheme } from 'react-native';
import { colorScheme as nativewindColorScheme } from 'nativewind';

import type { ThemePreference } from '@/types/api';
import { paletteFor, type Palette } from './palette';

interface ThemeContextValue {
  preference: ThemePreference;
  scheme: 'light' | 'dark';
  palette: Palette;
  /** Set the in-app preference. Persisting to the server is the caller's job. */
  setPreference: (pref: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const device = useDeviceColorScheme(); // 'light' | 'dark' | null
  const [preference, setPreference] = useState<ThemePreference>('SYSTEM');

  const scheme: 'light' | 'dark' = useMemo(() => {
    if (preference === 'LIGHT') return 'light';
    if (preference === 'DARK') return 'dark';
    return device === 'dark' ? 'dark' : 'light';
  }, [preference, device]);

  // Drive NativeWind's class-based dark mode so `dark:` variants respond.
  useEffect(() => {
    nativewindColorScheme.set(preference === 'SYSTEM' ? 'system' : scheme);
  }, [preference, scheme]);

  const value = useMemo<ThemeContextValue>(
    () => ({ preference, scheme, palette: paletteFor(scheme), setPreference }),
    [preference, scheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within a ThemeProvider');
  return ctx;
}
