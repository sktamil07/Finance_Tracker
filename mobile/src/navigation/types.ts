import type { NavigatorScreenParams } from '@react-navigation/native';

/**
 * Central navigation contract. Screens read their props from these param lists
 * so route names and params stay type-checked end to end.
 */

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

export type TabParamList = {
  Home: undefined;
  Invest: undefined;
  Expense: undefined;
  Tips: undefined;
};

export type AppStackParamList = {
  Tabs: NavigatorScreenParams<TabParamList> | undefined;
  Income: undefined;
  Reports: undefined;
  Settings: undefined;
  Categories: undefined;
  BudgetGoals: undefined;
};
