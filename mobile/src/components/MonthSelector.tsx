import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { addMonths, formatYearMonthLong, isCurrentOrFuture } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';

/**
 * Prev/next month stepper. "Next" is disabled once you reach the current month —
 * there is no data to show for the future.
 */
export function MonthSelector({
  month,
  onChange,
}: {
  month: string;
  onChange: (next: string) => void;
}) {
  const { palette } = useTheme();
  const atLatest = isCurrentOrFuture(month);

  return (
    <View className="flex-row items-center justify-between rounded-xl border border-gray-200 bg-white px-2 py-1.5 dark:border-slate-700/60 dark:bg-[#141C2B]">
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Previous month"
        onPress={() => onChange(addMonths(month, -1))}
        className="h-9 w-9 items-center justify-center rounded-lg active:bg-gray-100 dark:active:bg-slate-700"
      >
        <MaterialCommunityIcons name="chevron-left" size={26} color={palette.text} />
      </Pressable>

      <Text className="text-base font-semibold text-gray-900 dark:text-white">
        {formatYearMonthLong(month)}
      </Text>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Next month"
        disabled={atLatest}
        onPress={() => onChange(addMonths(month, 1))}
        className={`h-9 w-9 items-center justify-center rounded-lg active:bg-gray-100 dark:active:bg-slate-700 ${
          atLatest ? 'opacity-30' : ''
        }`}
      >
        <MaterialCommunityIcons name="chevron-right" size={26} color={palette.text} />
      </Pressable>
    </View>
  );
}
