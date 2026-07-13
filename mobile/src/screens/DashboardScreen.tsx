import React, { useState } from 'react';
import { Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useDashboard } from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { AllocationPie } from '@/components/charts/AllocationPie';
import { BreakdownBars, Legend, type BreakdownRow } from '@/components/charts/BreakdownBars';
import { MonthSelector } from '@/components/MonthSelector';
import { Card, SectionHeader } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { formatMoney, formatPercent } from '@/lib/money';
import { currentYearMonth, formatLocalDate } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';
import type { AppStackParamList } from '@/navigation/types';
import type { CategoryBreakdownItem, DashboardResponse } from '@/types/api';

function MetricCard({
  label,
  value,
  icon,
  tint,
  sub,
  onPress,
}: {
  label: string;
  value: string;
  icon: React.ComponentProps<typeof MaterialCommunityIcons>['name'];
  tint: string;
  sub?: string;
  onPress?: () => void;
}) {
  const body = (
    <Card className="mb-3 flex-1">
      <View className="flex-row items-center justify-between">
        <View className="flex-row items-center">
          <View
            style={{ backgroundColor: `${tint}22` }}
            className="mr-2 h-8 w-8 items-center justify-center rounded-lg"
          >
            <MaterialCommunityIcons name={icon} size={18} color={tint} />
          </View>
          <Text className="text-xs font-medium uppercase tracking-wide text-gray-500 dark:text-slate-400">
            {label}
          </Text>
        </View>
        {onPress ? (
          <MaterialCommunityIcons name="plus-circle-outline" size={18} color={tint} />
        ) : null}
      </View>
      <Text
        numberOfLines={1}
        adjustsFontSizeToFit
        className="mt-2 text-xl font-bold text-gray-900 dark:text-white"
      >
        {value}
      </Text>
      {sub ? <Text className="mt-0.5 text-xs text-gray-500 dark:text-slate-400">{sub}</Text> : null}
    </Card>
  );
  if (onPress) {
    return (
      <Pressable onPress={onPress} className="flex-1" accessibilityRole="button">
        {body}
      </Pressable>
    );
  }
  return body;
}

function toRows(items: CategoryBreakdownItem[], colors: string[]): BreakdownRow[] {
  return items.map((it, i) => ({
    key: it.categoryId,
    label: it.categoryName,
    icon: it.categoryIcon,
    amount: it.total,
    percentage: it.percentage,
    color: colors[i % colors.length],
  }));
}

function DashboardBody({ data }: { data: DashboardResponse }) {
  const { palette } = useTheme();
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const cc = data.currencyCode;
  const { metrics, allocation } = data;

  const allocationSegments = [
    { label: 'Invested', value: metrics.invested, color: palette.categorical[0] },
    { label: 'Expenses', value: metrics.expenses, color: palette.categorical[3] },
    { label: 'Savings', value: Math.max(0, metrics.savings), color: palette.categorical[1] },
  ];

  return (
    <View className="px-4 pb-8">
      {/* Metric cards */}
      <View className="mt-3 flex-row gap-3">
        <MetricCard
          label="Salary"
          value={formatMoney(metrics.salary, cc)}
          icon="cash"
          tint={palette.categorical[4]}
          sub="Tap to manage income"
          onPress={() => navigation.navigate('Income')}
        />
        <MetricCard label="Invested" value={formatMoney(metrics.invested, cc)} icon="chart-line" tint={palette.categorical[0]} />
      </View>
      <View className="flex-row gap-3">
        <MetricCard label="Expenses" value={formatMoney(metrics.expenses, cc)} icon="wallet-outline" tint={palette.categorical[3]} />
        <MetricCard
          label="Savings"
          value={formatMoney(metrics.savings, cc)}
          icon="piggy-bank-outline"
          tint={palette.categorical[1]}
          sub={`${formatPercent(metrics.savingsRate)} savings rate`}
        />
      </View>

      {/* Bond-due alerts */}
      {data.bondAlerts.length > 0 ? (
        <>
          <SectionHeader title="Maturity alerts" />
          {data.bondAlerts.map((b) => (
            <Card key={b.investmentId} className="mb-2 border-amber-300 dark:border-amber-500/40">
              <View className="flex-row items-start">
                <MaterialCommunityIcons
                  name="bell-alert-outline"
                  size={20}
                  color={palette.warning}
                  style={{ marginTop: 2 }}
                />
                <View className="ml-2 flex-1">
                  <Text className="font-semibold text-gray-900 dark:text-white">{b.name}</Text>
                  <Text className="text-sm text-gray-500 dark:text-slate-400">
                    {b.categoryName} · {formatMoney(b.amount, cc)}
                  </Text>
                  <Text className="mt-0.5 text-sm text-amber-700 dark:text-amber-300">
                    {b.maturingThisMonth ? 'Matures this month' : 'Matures'} on{' '}
                    {formatLocalDate(b.maturityDate)}
                  </Text>
                </View>
              </View>
            </Card>
          ))}
        </>
      ) : null}

      {/* Allocation */}
      <SectionHeader title="Allocation" />
      <Card>
        {metrics.salary <= 0 ? (
          <EmptyState
            icon="chart-donut"
            title="No allocation yet"
            subtitle="Add income and expenses for this month to see your split."
          />
        ) : (
          <View className="flex-row items-center">
            <AllocationPie
              segments={allocationSegments}
              centerSubtitle="Saved"
              centerTitle={formatPercent(metrics.savingsRate, 0)}
            />
            <View className="ml-4 flex-1">
              <Legend
                items={[
                  { label: 'Invested', color: palette.categorical[0], value: formatPercent(allocation.investedPercentage) },
                  { label: 'Expenses', color: palette.categorical[3], value: formatPercent(allocation.expensesPercentage) },
                  { label: 'Savings', color: palette.categorical[1], value: formatPercent(allocation.savingsPercentage) },
                ]}
              />
            </View>
          </View>
        )}
      </Card>

      {/* Expense breakdown */}
      <SectionHeader title="Expense breakdown" />
      <Card>
        {data.expenseBreakdown.length === 0 ? (
          <EmptyState icon="wallet-outline" title="No expenses" subtitle="Nothing recorded this month." />
        ) : (
          <BreakdownBars rows={toRows(data.expenseBreakdown, palette.categorical)} currencyCode={cc} />
        )}
      </Card>

      {/* Investment breakdown */}
      <SectionHeader title="Investment breakdown" />
      <Card>
        {data.investmentBreakdown.length === 0 ? (
          <EmptyState icon="chart-line" title="No investments" subtitle="Nothing recorded this month." />
        ) : (
          <BreakdownBars rows={toRows(data.investmentBreakdown, palette.categorical)} currencyCode={cc} />
        )}
      </Card>
    </View>
  );
}

export function DashboardScreen() {
  const [month, setMonth] = useState(currentYearMonth());
  const query = useDashboard(month);

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="px-4 pt-3">
        <MonthSelector month={month} onChange={setMonth} />
      </View>

      {query.isLoading ? (
        <LoadingState label="Loading dashboard…" />
      ) : query.isError ? (
        <ErrorState message={getApiErrorMessage(query.error)} onRetry={() => query.refetch()} />
      ) : query.data ? (
        <ScrollView
          refreshControl={
            <RefreshControl refreshing={query.isRefetching} onRefresh={() => query.refetch()} />
          }
        >
          <DashboardBody data={query.data} />
        </ScrollView>
      ) : null}
    </View>
  );
}
