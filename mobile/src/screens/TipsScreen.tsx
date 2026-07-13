import React from 'react';
import { Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { useTheme } from '@/theme/ThemeProvider';

/**
 * Tips is a defined tab in the SOP but has no specified content. Per the brief we
 * ship an honest placeholder rather than inventing financial advice.
 */
export function TipsScreen() {
  const { palette } = useTheme();
  return (
    <View className="flex-1 items-center justify-center bg-gray-100 px-10 dark:bg-[#0B1220]">
      <View
        style={{ backgroundColor: `${palette.brand}22` }}
        className="mb-5 h-20 w-20 items-center justify-center rounded-3xl"
      >
        <MaterialCommunityIcons name="lightbulb-on-outline" size={40} color={palette.brand} />
      </View>
      <Text className="text-center text-xl font-bold text-gray-900 dark:text-white">
        Tips are coming soon
      </Text>
      <Text className="mt-2 text-center text-gray-500 dark:text-slate-400">
        Personalised guidance for your money will appear here in a future update.
      </Text>
    </View>
  );
}
