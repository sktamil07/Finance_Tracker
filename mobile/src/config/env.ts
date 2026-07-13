import Constants from 'expo-constants';

/**
 * Resolves the API base URL from the environment. Precedence:
 *   1. EXPO_PUBLIC_API_BASE_URL (inlined by Metro at build time)
 *   2. expo config `extra.apiBaseUrl` (mirror, see app.config.ts)
 *
 * There is no hardcoded production fallback on purpose — a misconfigured build
 * should fail loudly in dev rather than silently talk to the wrong server.
 */
const fromEnv = process.env.EXPO_PUBLIC_API_BASE_URL;
const fromExtra = (Constants.expoConfig?.extra as { apiBaseUrl?: string } | undefined)?.apiBaseUrl;

export const API_BASE_URL = (fromEnv || fromExtra || '').replace(/\/+$/, '');

if (!API_BASE_URL && __DEV__) {
  // eslint-disable-next-line no-console
  console.warn(
    '[config] EXPO_PUBLIC_API_BASE_URL is not set. Copy .env.example to .env ' +
      'and point it at your backend, then restart the dev server.',
  );
}
