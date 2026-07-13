import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import type {
  BudgetGoalRequest,
  BudgetGoalResponse,
  BudgetStatusResponse,
  CategoryRequest,
  CategoryResponse,
  CategoryType,
  CategoryUpdateRequest,
  DashboardResponse,
  ExpenseRequest,
  ExpenseResponse,
  ExpensesByCategoryResponse,
  IncomeRequest,
  IncomeResponse,
  InvestmentRequest,
  InvestmentResponse,
  Page,
  ReportSummaryResponse,
  SettingsResponse,
  UpcomingInvestmentResponse,
  UpdateSettingsRequest,
} from '@/types/api';
import { api } from './client';
import { qk } from './queryClient';

// ---- Dashboard -----------------------------------------------------------

export function useDashboard(month: string) {
  return useQuery({
    queryKey: qk.dashboard(month),
    queryFn: async () => {
      const { data } = await api.get<DashboardResponse>('/dashboard', { params: { month } });
      return data;
    },
  });
}

// ---- Categories ----------------------------------------------------------

export function useCategories(type?: CategoryType) {
  return useQuery({
    queryKey: qk.categories(type),
    queryFn: async () => {
      const { data } = await api.get<CategoryResponse[]>('/categories', {
        params: type ? { type } : undefined,
      });
      return data;
    },
  });
}

export function useCreateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: CategoryRequest) => {
      const { data } = await api.post<CategoryResponse>('/categories', body);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  });
}

export function useUpdateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: CategoryUpdateRequest }) => {
      const { data } = await api.put<CategoryResponse>(`/categories/${id}`, body);
      return data;
    },
    onSuccess: () => {
      // A rename shows up on every screen that carries a category name.
      qc.invalidateQueries();
    },
  });
}

export function useDeleteCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/categories/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  });
}

// ---- Investments ---------------------------------------------------------

export function useInvestments(month?: string) {
  return useQuery({
    queryKey: qk.investments(month),
    queryFn: async () => {
      const { data } = await api.get<InvestmentResponse[]>('/investments', {
        params: month ? { month } : undefined,
      });
      return data;
    },
  });
}

export function useUpcomingInvestments() {
  return useQuery({
    queryKey: qk.investmentsUpcoming,
    queryFn: async () => {
      const { data } = await api.get<UpcomingInvestmentResponse[]>('/investments/upcoming');
      return data;
    },
  });
}

function invalidateInvestmentViews(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['investments'] });
  qc.invalidateQueries({ queryKey: ['dashboard'] });
  qc.invalidateQueries({ queryKey: ['reports'] });
}

export function useCreateInvestment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: InvestmentRequest) => {
      const { data } = await api.post<InvestmentResponse>('/investments', body);
      return data;
    },
    onSuccess: () => invalidateInvestmentViews(qc),
  });
}

export function useUpdateInvestment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: InvestmentRequest }) => {
      const { data } = await api.put<InvestmentResponse>(`/investments/${id}`, body);
      return data;
    },
    onSuccess: () => invalidateInvestmentViews(qc),
  });
}

export function useDeleteInvestment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/investments/${id}`);
    },
    onSuccess: () => invalidateInvestmentViews(qc),
  });
}

// ---- Expenses ------------------------------------------------------------

export function useExpensesGrouped(month: string) {
  return useQuery({
    queryKey: qk.expensesGrouped(month),
    queryFn: async () => {
      const { data } = await api.get<ExpensesByCategoryResponse>('/expenses', {
        params: { month },
      });
      return data;
    },
  });
}

const TX_PAGE_SIZE = 20;

export function useExpenseTransactions(month?: string) {
  return useInfiniteQuery({
    queryKey: qk.expenseTransactions(month),
    initialPageParam: 0,
    queryFn: async ({ pageParam }) => {
      const { data } = await api.get<Page<ExpenseResponse>>('/expenses/transactions', {
        params: {
          ...(month ? { month } : {}),
          page: pageParam,
          size: TX_PAGE_SIZE,
          sort: 'transactionDate,desc',
        },
      });
      return data;
    },
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
  });
}

function invalidateExpenseViews(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['expenses'] });
  qc.invalidateQueries({ queryKey: ['dashboard'] });
  qc.invalidateQueries({ queryKey: ['budgets'] });
  qc.invalidateQueries({ queryKey: ['reports'] });
}

export function useCreateExpense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ExpenseRequest) => {
      const { data } = await api.post<ExpenseResponse>('/expenses', body);
      return data;
    },
    onSuccess: () => invalidateExpenseViews(qc),
  });
}

export function useUpdateExpense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: ExpenseRequest }) => {
      const { data } = await api.put<ExpenseResponse>(`/expenses/${id}`, body);
      return data;
    },
    onSuccess: () => invalidateExpenseViews(qc),
  });
}

export function useDeleteExpense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/expenses/${id}`);
    },
    onSuccess: () => invalidateExpenseViews(qc),
  });
}

// ---- Budgets -------------------------------------------------------------

export function useBudgets(month: string) {
  return useQuery({
    queryKey: qk.budgets(month),
    queryFn: async () => {
      const { data } = await api.get<BudgetStatusResponse[]>('/budgets', { params: { month } });
      return data;
    },
  });
}

export function useBudgetGoals() {
  return useQuery({
    queryKey: qk.budgetGoals,
    queryFn: async () => {
      const { data } = await api.get<BudgetGoalResponse[]>('/budgets/goals');
      return data;
    },
  });
}

function invalidateBudgetViews(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['budgets'] });
}

export function useCreateBudgetGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: BudgetGoalRequest) => {
      const { data } = await api.post<BudgetGoalResponse>('/budgets', body);
      return data;
    },
    onSuccess: () => invalidateBudgetViews(qc),
  });
}

export function useUpdateBudgetGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: BudgetGoalRequest }) => {
      const { data } = await api.put<BudgetGoalResponse>(`/budgets/${id}`, body);
      return data;
    },
    onSuccess: () => invalidateBudgetViews(qc),
  });
}

export function useDeleteBudgetGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/budgets/${id}`);
    },
    onSuccess: () => invalidateBudgetViews(qc),
  });
}

// ---- Report --------------------------------------------------------------

export function useReport(month: string, months: number) {
  return useQuery({
    queryKey: qk.report(month, months),
    queryFn: async () => {
      const { data } = await api.get<ReportSummaryResponse>('/reports/summary', {
        params: { month, months },
      });
      return data;
    },
  });
}

// ---- Settings ------------------------------------------------------------

export function useSettings() {
  return useQuery({
    queryKey: qk.settings,
    queryFn: async () => {
      const { data } = await api.get<SettingsResponse>('/settings');
      return data;
    },
  });
}

export function useUpdateSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: UpdateSettingsRequest) => {
      const { data } = await api.put<SettingsResponse>('/settings', body);
      return data;
    },
    onSuccess: (data) => {
      qc.setQueryData(qk.settings, data);
      // Currency change ripples into every money figure.
      qc.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

// ---- Income --------------------------------------------------------------

export function useIncome(month?: string) {
  return useQuery({
    queryKey: qk.income(month),
    queryFn: async () => {
      const { data } = await api.get<IncomeResponse[]>('/income', {
        params: month ? { month } : undefined,
      });
      return data;
    },
  });
}

function invalidateIncomeViews(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['income'] });
  qc.invalidateQueries({ queryKey: ['dashboard'] });
  qc.invalidateQueries({ queryKey: ['reports'] });
}

export function useCreateIncome() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: IncomeRequest) => {
      const { data } = await api.post<IncomeResponse>('/income', body);
      return data;
    },
    onSuccess: () => invalidateIncomeViews(qc),
  });
}

export function useUpdateIncome() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: IncomeRequest }) => {
      const { data } = await api.put<IncomeResponse>(`/income/${id}`, body);
      return data;
    },
    onSuccess: () => invalidateIncomeViews(qc),
  });
}

export function useDeleteIncome() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/income/${id}`);
    },
    onSuccess: () => invalidateIncomeViews(qc),
  });
}
