import React from 'react';
import { Pressable, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useTheme } from '@/theme/ThemeProvider';
import type { AppStackParamList } from '@/navigation/types';

/**
 * Header actions shown on every tab: quick access to Reports and Settings,
 * which live in the app-level stack outside the bottom tab bar.
 */
export function HeaderMenu() {
  const { palette } = useTheme();
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();

  return (
    <View className="flex-row items-center">
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Reports"
        hitSlop={8}
        onPress={() => navigation.navigate('Reports')}
        className="mr-1 h-10 w-10 items-center justify-center rounded-full active:bg-gray-100 dark:active:bg-slate-700"
      >
        <MaterialCommunityIcons name="chart-timeline-variant" size={22} color={palette.text} />
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Settings"
        hitSlop={8}
        onPress={() => navigation.navigate('Settings')}
        className="h-10 w-10 items-center justify-center rounded-full active:bg-gray-100 dark:active:bg-slate-700"
      >
        <MaterialCommunityIcons name="cog-outline" size={22} color={palette.text} />
      </Pressable>
    </View>
  );
}
