import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios';

import { API_BASE_URL } from '@/config/env';
import type { ApiError, TokenResponse } from '@/types/api';

/**
 * Single Axios instance shared by the whole app. Interceptors mirror the web
 * client:
 *   • request  — attach `Authorization: Bearer <accessToken>` when present.
 *   • response — on a 401, refresh ONCE (single-flight), replay the original
 *                request, and if refresh itself fails, hard-logout.
 *
 * Tokens are held in module memory for synchronous interceptor access; the
 * durable copy lives in SecureStore (see tokenStorage.ts). AuthContext keeps
 * the two in sync via setAuthTokens / registerAuthHandlers.
 */

let accessToken: string | null = null;
let refreshToken: string | null = null;

/** Called after a successful silent refresh so the durable store is updated. */
let onTokensRefreshed: ((tokens: TokenResponse) => void) | null = null;
/** Called when refresh fails / is impossible — triggers auto-logout. */
let onAuthFailure: (() => void) | null = null;

export function setAuthTokens(tokens: { accessToken: string; refreshToken: string } | null): void {
  accessToken = tokens?.accessToken ?? null;
  refreshToken = tokens?.refreshToken ?? null;
}

export function registerAuthHandlers(handlers: {
  onTokensRefreshed: (tokens: TokenResponse) => void;
  onAuthFailure: () => void;
}): void {
  onTokensRefreshed = handlers.onTokensRefreshed;
  onAuthFailure = handlers.onAuthFailure;
}

export const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// A bare instance for the refresh call itself, so the response interceptor
// below can never recurse into refreshing while refreshing.
const refreshClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${accessToken}`);
    config.headers = headers;
  }
  return config;
});

// ---- single-flight refresh ------------------------------------------------

let refreshInFlight: Promise<string> | null = null;

async function performRefresh(): Promise<string> {
  if (!refreshToken) throw new Error('No refresh token');
  const { data } = await refreshClient.post<TokenResponse>('/auth/refresh', {
    refreshToken,
  });
  accessToken = data.accessToken;
  refreshToken = data.refreshToken;
  onTokensRefreshed?.(data);
  return data.accessToken;
}

function refreshOnce(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;

    const isAuthEndpoint = original?.url?.includes('/auth/');
    const canRetry = status === 401 && original && !original._retry && !isAuthEndpoint;

    if (canRetry) {
      original._retry = true;
      try {
        const fresh = await refreshOnce();
        const headers = AxiosHeaders.from(original.headers);
        headers.set('Authorization', `Bearer ${fresh}`);
        original.headers = headers;
        return api(original);
      } catch (refreshError) {
        // Refresh failed → session is unrecoverable. Auto-logout.
        onAuthFailure?.();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);
