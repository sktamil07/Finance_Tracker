import { QueryClient } from '@tanstack/react-query';
import axios from 'axios';

/**
 * TanStack Query is the single source of truth for server state. Defaults:
 *   • don't retry 4xx (a 400/403/404 won't fix itself); retry other errors once
 *   • 30s stale time so tab-switching doesn't refetch constantly
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status;
          if (status && status >= 400 && status < 500) return false;
        }
        return failureCount < 1;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

/** Centralised query keys so invalidation stays consistent across screens. */
export const qk = {
  me: ['me'] as const,
  settings: ['settings'] as const,
  dashboard: (month: string) => ['dashboard', month] as const,
  categories: (type?: string) => ['categories', type ?? 'ALL'] as const,
  investments: (month?: string) => ['investments', month ?? 'ALL'] as const,
  investmentsUpcoming: ['investments', 'upcoming'] as const,
  expensesGrouped: (month: string) => ['expenses', 'grouped', month] as const,
  expenseTransactions: (month?: string) => ['expenses', 'transactions', month ?? 'ALL'] as const,
  budgets: (month: string) => ['budgets', month] as const,
  budgetGoals: ['budgets', 'goals'] as const,
  report: (month: string, months: number) => ['reports', month, months] as const,
  income: (month?: string) => ['income', month ?? 'ALL'] as const,
};
