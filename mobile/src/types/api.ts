/**
 * Wire contract shared with the Spring Boot backend (`/api/v1`).
 *
 * These types are the hand-maintained mirror of the OpenAPI schema served at
 * `GET /v3/api-docs`. They intentionally use the SAME field names the API emits
 * so web and mobile share one contract. To regenerate from the live spec:
 *
 *   npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/openapi.d.ts
 *
 * and swap these aliases for the generated `components['schemas'][…]`. Kept hand
 * written here so the app compiles without a running backend.
 *
 * Money is DECIMAL(15,2) on the server and serialised as a JSON number; we keep
 * it as `number` on the wire and format at the edge with Intl.NumberFormat.
 * A `month` is the string "YYYY-MM" (java.time.YearMonth); a date is "YYYY-MM-DD".
 */

export type YearMonthString = string; // "2026-04"
export type LocalDateString = string; // "2026-04-12"
export type InstantString = string; // ISO-8601 "2026-04-12T09:31:04.221Z"

export type CategoryType = 'EXPENSE' | 'INVESTMENT';
export type ThemePreference = 'LIGHT' | 'DARK' | 'SYSTEM';
export type BudgetStatus = 'NONE' | 'WARNING' | 'EXCEEDED';
export type UpcomingKind = 'SIP' | 'MATURITY';

// ---- Auth ----------------------------------------------------------------

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  expiresIn: number; // seconds
}

// ---- User / Settings -----------------------------------------------------

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  currencyCode: string;
  themePreference: ThemePreference;
  createdAt: InstantString;
  updatedAt: InstantString;
}

export interface SettingsResponse {
  displayName: string;
  email: string;
  currencyCode: string;
  themePreference: ThemePreference;
}

export interface UpdateSettingsRequest {
  displayName: string;
  currencyCode: string;
  themePreference: ThemePreference;
}

// ---- Category ------------------------------------------------------------

export interface CategoryResponse {
  id: number;
  name: string;
  type: CategoryType;
  icon: string | null;
  systemDefault: boolean;
  createdAt: InstantString;
}

export interface CategoryRequest {
  name: string;
  type: CategoryType;
  icon?: string | null;
}

export interface CategoryUpdateRequest {
  name: string;
  icon?: string | null;
}

// ---- Dashboard -----------------------------------------------------------

export interface DashboardMetrics {
  salary: number;
  invested: number;
  expenses: number;
  savings: number;
  savingsRate: number;
}

export interface AllocationResponse {
  investedPercentage: number;
  expensesPercentage: number;
  savingsPercentage: number;
}

export interface CategoryBreakdownItem {
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  total: number;
  percentage: number;
}

export interface BondAlertResponse {
  investmentId: number;
  name: string;
  categoryName: string;
  amount: number;
  maturityDate: LocalDateString;
  maturingThisMonth: boolean;
  roiNotes: string | null;
}

export interface DashboardResponse {
  month: YearMonthString;
  currencyCode: string;
  metrics: DashboardMetrics;
  allocation: AllocationResponse;
  expenseBreakdown: CategoryBreakdownItem[];
  investmentBreakdown: CategoryBreakdownItem[];
  bondAlerts: BondAlertResponse[];
}

// ---- Income --------------------------------------------------------------

export interface IncomeRequest {
  month: YearMonthString;
  amount: number;
  source?: string;
}

export interface IncomeResponse {
  id: number;
  month: YearMonthString;
  amount: number;
  source: string;
  createdAt: InstantString;
}

// ---- Investment ----------------------------------------------------------

export interface InvestmentRequest {
  name: string;
  amount: number;
  categoryId: number;
  month: YearMonthString;
  roiNotes?: string | null;
  recurring: boolean;
  recurringDayOfMonth?: number | null;
  maturityDate?: LocalDateString | null;
}

export interface InvestmentResponse {
  id: number;
  name: string;
  amount: number;
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  month: YearMonthString;
  roiNotes: string | null;
  recurring: boolean;
  recurringDayOfMonth: number | null;
  maturityDate: LocalDateString | null;
  createdAt: InstantString;
  updatedAt: InstantString;
}

export interface UpcomingInvestmentResponse {
  investmentId: number;
  name: string;
  categoryName: string;
  amount: number;
  kind: UpcomingKind;
  dueDate: LocalDateString;
  roiNotes: string | null;
}

// ---- Expense -------------------------------------------------------------

export interface ExpenseRequest {
  description: string;
  amount: number;
  categoryId: number;
  transactionDate: LocalDateString;
  notes?: string | null;
}

export interface ExpenseResponse {
  id: number;
  description: string;
  amount: number;
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  transactionDate: LocalDateString;
  notes: string | null;
  createdAt: InstantString;
  updatedAt: InstantString;
}

export interface ExpenseCategoryGroup {
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  total: number;
  percentageOfMonth: number;
  transactions: ExpenseResponse[];
}

export interface ExpensesByCategoryResponse {
  month: YearMonthString;
  totalExpense: number;
  categories: ExpenseCategoryGroup[];
}

// ---- Budget --------------------------------------------------------------

export interface BudgetGoalRequest {
  categoryId: number;
  limitAmount: number;
  month?: YearMonthString | null;
}

export interface BudgetGoalResponse {
  id: number;
  categoryId: number;
  categoryName: string;
  limitAmount: number;
  month: YearMonthString | null;
  recurring: boolean;
  createdAt: InstantString;
}

export interface BudgetStatusResponse {
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  month: YearMonthString;
  spent: number;
  limitAmount: number;
  remaining: number;
  utilisationPercentage: number;
  status: BudgetStatus;
  appliedGoalId: number | null;
  recurringGoalApplied: boolean;
}

// ---- Report --------------------------------------------------------------

export interface MonthlyPoint {
  month: YearMonthString;
  salary: number;
  invested: number;
  expenses: number;
  savings: number;
  savingsRate: number;
  cumulativeInvested: number;
}

export interface SavingsRatePoint {
  month: YearMonthString;
  savingsRate: number;
}

export interface TopExpenseCategory {
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  total: number;
  percentageOfWindow: number;
}

export interface ReportTotals {
  salary: number;
  invested: number;
  expenses: number;
  savings: number;
  savingsRate: number;
}

export interface ReportSummaryResponse {
  fromMonth: YearMonthString;
  toMonth: YearMonthString;
  months: number;
  monthly: MonthlyPoint[];
  savingsRateTrend: SavingsRatePoint[];
  topExpenseCategories: TopExpenseCategory[];
  totals: ReportTotals;
}

// ---- Errors / pagination -------------------------------------------------

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: InstantString;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: ApiFieldError[];
}

/** Spring Data `Page<T>` envelope (subset the client relies on). */
export interface Page<T> {
  content: T[];
  number: number; // current page index (0-based)
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}
