import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/**
 * Token persistence. Tokens live in the device keychain (iOS Keychain /
 * Android Keystore-backed EncryptedSharedPreferences) via expo-secure-store —
 * NEVER in AsyncStorage, which is plaintext on disk.
 *
 * SecureStore values are capped at ~2 KB each; JWTs are well under that.
 */

const ACCESS_KEY = 'ft.accessToken';
const REFRESH_KEY = 'ft.refreshToken';

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
}

function browserStorage(): Storage | null {
  if (Platform.OS !== 'web' || typeof window === 'undefined') return null;
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export async function saveTokens(tokens: StoredTokens): Promise<void> {
  const storage = browserStorage();
  if (storage) {
    try {
      storage.setItem(ACCESS_KEY, tokens.accessToken);
      storage.setItem(REFRESH_KEY, tokens.refreshToken);
    } catch {
      // Authentication can still proceed for this browser session without
      // persistence (for example, when private-mode storage is disabled).
    }
    return;
  }
  await Promise.all([
    SecureStore.setItemAsync(ACCESS_KEY, tokens.accessToken),
    SecureStore.setItemAsync(REFRESH_KEY, tokens.refreshToken),
  ]);
}

export async function loadTokens(): Promise<StoredTokens | null> {
  const storage = browserStorage();
  if (storage) {
    try {
      const accessToken = storage.getItem(ACCESS_KEY);
      const refreshToken = storage.getItem(REFRESH_KEY);
      return accessToken && refreshToken ? { accessToken, refreshToken } : null;
    } catch {
      return null;
    }
  }
  if (Platform.OS === 'web') return null;
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync(ACCESS_KEY),
    SecureStore.getItemAsync(REFRESH_KEY),
  ]);
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

export async function clearTokens(): Promise<void> {
  const storage = browserStorage();
  if (storage) {
    try {
      storage.removeItem(ACCESS_KEY);
      storage.removeItem(REFRESH_KEY);
    } catch {
      // There is no persistent browser session to remove.
    }
    return;
  }
  if (Platform.OS === 'web') return;
  await Promise.all([
    SecureStore.deleteItemAsync(ACCESS_KEY),
    SecureStore.deleteItemAsync(REFRESH_KEY),
  ]);
}
