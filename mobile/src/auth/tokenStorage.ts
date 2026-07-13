import * as SecureStore from 'expo-secure-store';

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

export async function saveTokens(tokens: StoredTokens): Promise<void> {
  await Promise.all([
    SecureStore.setItemAsync(ACCESS_KEY, tokens.accessToken),
    SecureStore.setItemAsync(REFRESH_KEY, tokens.refreshToken),
  ]);
}

export async function loadTokens(): Promise<StoredTokens | null> {
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync(ACCESS_KEY),
    SecureStore.getItemAsync(REFRESH_KEY),
  ]);
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

export async function clearTokens(): Promise<void> {
  await Promise.all([
    SecureStore.deleteItemAsync(ACCESS_KEY),
    SecureStore.deleteItemAsync(REFRESH_KEY),
  ]);
}
