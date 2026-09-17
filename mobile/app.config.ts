import type { ExpoConfig, ConfigContext } from 'expo/config';

/**
 * Expo app config. The API base URL is NOT hardcoded — it is read from the
 * environment at build time so the same binary can point at dev / staging / prod.
 *
 *   EXPO_PUBLIC_API_BASE_URL   e.g. http://192.168.1.10:8080/api/v1
 *
 * On a physical device `localhost` is the phone, not your machine — use your
 * machine's LAN IP. On the Android emulator use http://10.0.2.2:8080/api/v1.
 * See README for the full matrix.
 */
export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Finance Tracker',
  slug: 'finance-tracker',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  scheme: 'financetracker',
  userInterfaceStyle: 'automatic',
  newArchEnabled: true,
  splash: {
    image: './assets/splash.png',
    resizeMode: 'contain',
    backgroundColor: '#0B1220',
  },
  assetBundlePatterns: ['**/*'],
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'tech.devopslabs.financetracker',
  },
  android: {
    package: 'tech.devopslabs.financetracker',
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon.png',
      backgroundColor: '#0B1220',
    },
    // Cleartext HTTP is allowed by default in dev builds; a real prod build should
    // point EXPO_PUBLIC_API_BASE_URL at an https:// origin.
  },
  web: {
    bundler: 'metro',
    favicon: './assets/favicon.png',
  },
  plugins: ['expo-secure-store', 'expo-system-ui'],
  extra: {
    // Mirror the public env var so it is also reachable via expo-constants if
    // a consumer prefers Constants.expoConfig.extra over process.env.
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL ?? '',
    // EAS project id. `eas init` creates the project and prints this id; because
    // this config is dynamic, paste it here (or export EAS_PROJECT_ID) so cloud
    // builds can link to your Expo account's project.
    eas: {
      projectId: process.env.EAS_PROJECT_ID,
    },
  },
});
