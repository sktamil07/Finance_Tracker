import React from 'react';
import { Text, View } from 'react-native';

import { CategoryIcon } from '@/components/CategoryIcon';
import { formatMoney, formatPercent } from '@/lib/money';

export interface BreakdownRow {
  key: string | number;
  label: string;
  icon?: string | null;
  amount: number;
  percentage: number;
  color: string;
}

/**
 * Horizontal proportion bars for a category breakdown. Pure views (no chart
 * lib) so it composes anywhere and animates cheaply.
 */
export function BreakdownBars({
  rows,
  currencyCode,
}: {
  rows: BreakdownRow[];
  currencyCode: string;
}) {
  return (
    <View className="gap-3">
      {rows.map((row) => (
        <View key={row.key}>
          <View className="mb-1 flex-row items-center justify-between">
            <View className="flex-1 flex-row items-center pr-2">
              {row.icon !== undefined ? (
                <CategoryIcon slug={row.icon} color={row.color} size={16} />
              ) : null}
              <Text
                numberOfLines={1}
                className="ml-2 flex-1 text-sm font-medium text-gray-800 dark:text-slate-100"
              >
                {row.label}
              </Text>
            </View>
            <Text className="text-sm font-semibold text-gray-900 dark:text-white">
              {formatMoney(row.amount, currencyCode)}
            </Text>
          </View>
          <View className="h-2 flex-row items-center rounded-full bg-gray-100 dark:bg-slate-700/60">
            <View
              style={{
                width: `${Math.min(100, Math.max(2, row.percentage))}%`,
                backgroundColor: row.color,
              }}
              className="h-2 rounded-full"
            />
          </View>
          <Text className="mt-0.5 text-right text-xs text-gray-500 dark:text-slate-400">
            {formatPercent(row.percentage)}
          </Text>
        </View>
      ))}
    </View>
  );
}

/** Small colored-dot legend used beside the allocation donut. */
export function Legend({
  items,
}: {
  items: { label: string; color: string; value?: string }[];
}) {
  return (
    <View className="gap-2">
      {items.map((item) => (
        <View key={item.label} className="flex-row items-center justify-between">
          <View className="flex-row items-center">
            <View style={{ backgroundColor: item.color }} className="mr-2 h-3 w-3 rounded-full" />
            <Text className="text-sm text-gray-700 dark:text-slate-200">{item.label}</Text>
          </View>
          {item.value ? (
            <Text className="text-sm font-semibold text-gray-900 dark:text-white">
              {item.value}
            </Text>
          ) : null}
        </View>
      ))}
    </View>
  );
}
