import React, { useState } from 'react';
import { Alert, RefreshControl, ScrollView, Text, View } from 'react-native';

import { useReport } from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useAuth, useCurrency } from '@/auth/AuthContext';
import { BreakdownBars, type BreakdownRow } from '@/components/charts/BreakdownBars';
import { TrendChart, type TrendPoint } from '@/components/charts/TrendChart';
import { MonthSelector } from '@/components/MonthSelector';
import { Button, Card, SectionHeader, Segmented } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { formatMoney, formatPercent } from '@/lib/money';
import { currentYearMonth, formatMonthAbbrev } from '@/lib/month';
import { exportReportPdf } from '@/lib/reportExport';
import { useTheme } from '@/theme/ThemeProvider';

function TotalRow({ label, value }: { label: string; value: string }) {
  return (
    <View className="flex-row items-center justify-between py-2">
      <Text className="text-gray-500 dark:text-slate-400">{label}</Text>
      <Text className="font-semibold text-gray-900 dark:text-white">{value}</Text>
    </View>
  );
}

export function ReportsScreen() {
  const [month, setMonth] = useState(currentYearMonth());
  const [months, setMonths] = useState<number>(6);
  const [exporting, setExporting] = useState(false);
  const currency = useCurrency();
  const { displayName } = useAuth();
  const { palette } = useTheme();

  const query = useReport(month, months);

  const onDownload = async () => {
    if (!query.data) return;
    setExporting(true);
    try {
      await exportReportPdf(query.data, currency, displayName);
    } catch (err) {
      Alert.alert('Export failed', getApiErrorMessage(err, 'Could not generate the PDF.'));
    } finally {
      setExporting(false);
    }
  };

  const trend: TrendPoint[] =
    query.data?.savingsRateTrend.map((p) => ({
      label: formatMonthAbbrev(p.month),
      value: p.savingsRate,
    })) ?? [];

  const topRows: BreakdownRow[] =
    query.data?.topExpenseCategories.map((c, i) => ({
      key: c.categoryId,
      label: c.categoryName,
      icon: c.categoryIcon,
      amount: c.total,
      percentage: c.percentageOfWindow,
      color: palette.categorical[i % palette.categorical.length],
    })) ?? [];

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="gap-3 px-4 pt-3">
        <MonthSelector month={month} onChange={setMonth} />
        <Segmented
          value={months}
          onChange={setMonths}
          options={[
            { label: '1 mo', value: 1 },
            { label: '3 mo', value: 3 },
            { label: '6 mo', value: 6 },
            { label: '12 mo', value: 12 },
          ]}
        />
      </View>

      {query.isLoading ? (
        <LoadingState label="Building report…" />
      ) : query.isError ? (
        <ErrorState message={getApiErrorMessage(query.error)} onRetry={() => query.refetch()} />
      ) : query.data ? (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingTop: 12 }}
          refreshControl={
            <RefreshControl refreshing={query.isRefetching} onRefresh={() => query.refetch()} />
          }
        >
          <Text className="mb-3 text-sm text-gray-500 dark:text-slate-400">
            {query.data.months === 1
              ? `Single month · ${query.data.toMonth}`
              : `${query.data.months}-month window · ${query.data.fromMonth} → ${query.data.toMonth}`}
          </Text>

          <Button
            label={exporting ? 'Preparing…' : 'Download report (PDF)'}
            onPress={onDownload}
            loading={exporting}
            variant="secondary"
            fullWidth
          />

          <SectionHeader title="Totals" />
          <Card>
            <TotalRow label="Salary" value={formatMoney(query.data.totals.salary, currency)} />
            <TotalRow label="Invested" value={formatMoney(query.data.totals.invested, currency)} />
            <TotalRow label="Expenses" value={formatMoney(query.data.totals.expenses, currency)} />
            <TotalRow label="Savings" value={formatMoney(query.data.totals.savings, currency)} />
            <View className="mt-1 border-t border-gray-100 pt-2 dark:border-slate-700/60">
              <TotalRow
                label="Avg savings rate"
                value={formatPercent(query.data.totals.savingsRate)}
              />
            </View>
          </Card>

          <SectionHeader title="Savings rate trend" />
          <Card>
            {trend.length < 2 ? (
              <EmptyState icon="chart-line" title="Not enough history" />
            ) : (
              <TrendChart points={trend} suffix="%" />
            )}
          </Card>

          <SectionHeader title="Top expense categories" />
          <Card>
            {topRows.length === 0 ? (
              <EmptyState icon="wallet-outline" title="No expenses in this window" />
            ) : (
              <BreakdownBars rows={topRows} currencyCode={currency} />
            )}
          </Card>
        </ScrollView>
      ) : null}
    </View>
  );
}
