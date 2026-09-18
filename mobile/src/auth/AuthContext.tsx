import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import { api, registerAuthHandlers, setAuthTokens } from '@/api/client';
import { queryClient } from '@/api/queryClient';
import { useTheme } from '@/theme/ThemeProvider';
import type {
  LoginRequest,
  RegisterRequest,
  SettingsResponse,
  ThemePreference,
  TokenResponse,
  UserResponse,
} from '@/types/api';
import { clearTokens, loadTokens, saveTokens } from './tokenStorage';

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

interface AuthContextValue {
  status: AuthStatus;
  user: UserResponse | null;
  currencyCode: string;
  displayName: string;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  /** Reflect a settings change (currency / display name / theme) into auth state. */
  applySettings: (settings: SettingsResponse) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { setPreference } = useTheme();
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<UserResponse | null>(null);
  // Local overrides that may be fresher than the last /users/me (e.g. after a
  // settings PUT) — keeps currency/display name reactive without a refetch.
  const [displayName, setDisplayName] = useState<string>('');
  const [currencyCode, setCurrencyCode] = useState<string>('INR');

  // Avoid re-entrant logout loops when several requests 401 at once.
  const loggingOut = useRef(false);

  const finishLogout = useCallback(async () => {
    if (loggingOut.current) return;
    loggingOut.current = true;
    setAuthTokens(null);
    await clearTokens();
    queryClient.clear();
    setUser(null);
    setStatus('unauthenticated');
    loggingOut.current = false;
  }, []);

  const hydrateFromUser = useCallback(
    (u: UserResponse) => {
      setUser(u);
      setDisplayName(u.name);
      setCurrencyCode(u.currencyCode);
      setPreference(u.themePreference);
    },
    [setPreference],
  );

  // Register the client's refresh/failure callbacks exactly once.
  useEffect(() => {
    registerAuthHandlers({
      onTokensRefreshed: (tokens: TokenResponse) => {
        // Keep the durable copy in sync after a silent refresh.
        void saveTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
      },
      onAuthFailure: () => {
        void finishLogout();
      },
    });
  }, [finishLogout]);

  // Bootstrap: restore a session from the keychain, if any.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const tokens = await loadTokens();
        if (!tokens) {
          if (!cancelled) setStatus('unauthenticated');
          return;
        }
        setAuthTokens(tokens);
        const { data } = await api.get<UserResponse>('/users/me');
        if (cancelled) return;
        hydrateFromUser(data);
        setStatus('authenticated');
      } catch {
        // Access token dead and refresh failed during /users/me → clean slate.
        if (!cancelled) await finishLogout();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [finishLogout, hydrateFromUser]);

  const establishSession = useCallback(
    async (tokens: TokenResponse) => {
      const pair = { accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
      setAuthTokens(pair);
      await saveTokens(pair);
      const { data } = await api.get<UserResponse>('/users/me');
      hydrateFromUser(data);
      setStatus('authenticated');
    },
    [hydrateFromUser],
  );

  const login = useCallback(
    async (credentials: LoginRequest) => {
      const { data } = await api.post<TokenResponse>('/auth/login', credentials);
      await establishSession(data);
    },
    [establishSession],
  );

  const register = useCallback(
    async (payload: RegisterRequest) => {
      const { data } = await api.post<TokenResponse>('/auth/register', payload);
      await establishSession(data);
    },
    [establishSession],
  );

  const logout = useCallback(async () => {
    await finishLogout();
  }, [finishLogout]);

  const applySettings = useCallback(
    (settings: SettingsResponse) => {
      setDisplayName(settings.displayName);
      setCurrencyCode(settings.currencyCode);
      setPreference(settings.themePreference);
      setUser((prev) =>
        prev
          ? {
              ...prev,
              name: settings.displayName,
              currencyCode: settings.currencyCode,
              themePreference: settings.themePreference,
            }
          : prev,
      );
    },
    [setPreference],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, currencyCode, displayName, login, register, logout, applySettings }),
    [status, user, currencyCode, displayName, login, register, logout, applySettings],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

/** Convenience: the active currency code, defaulting to INR. */
export function useCurrency(): string {
  return useAuth().currencyCode || 'INR';
}
