import React from 'react';
import { ActivityIndicator, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { useTheme } from '@/theme/ThemeProvider';
import { Button } from './ui';

/** Full-bleed spinner for first loads. */
export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  const { palette } = useTheme();
  return (
    <View className="flex-1 items-center justify-center p-8">
      <ActivityIndicator size="large" color={palette.brand} />
      <Text className="mt-3 text-gray-500 dark:text-slate-400">{label}</Text>
    </View>
  );
}

/** Error panel with a retry affordance. */
export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  const { palette } = useTheme();
  return (
    <View className="flex-1 items-center justify-center p-8">
      <MaterialCommunityIcons name="alert-circle-outline" size={44} color={palette.danger} />
      <Text className="mt-3 text-center text-base font-medium text-gray-800 dark:text-slate-100">
        Couldn’t load this
      </Text>
      <Text className="mt-1 text-center text-gray-500 dark:text-slate-400">{message}</Text>
      {onRetry ? (
        <View className="mt-5">
          <Button label="Try again" onPress={onRetry} variant="secondary" />
        </View>
      ) : null}
    </View>
  );
}

/** Empty placeholder for lists/sections with no rows. */
export function EmptyState({
  icon = 'inbox-outline',
  title,
  subtitle,
}: {
  icon?: React.ComponentProps<typeof MaterialCommunityIcons>['name'];
  title: string;
  subtitle?: string;
}) {
  const { palette } = useTheme();
  return (
    <View className="items-center justify-center px-8 py-12">
      <MaterialCommunityIcons name={icon} size={40} color={palette.textMuted} />
      <Text className="mt-3 text-center text-base font-medium text-gray-700 dark:text-slate-200">
        {title}
      </Text>
      {subtitle ? (
        <Text className="mt-1 text-center text-gray-500 dark:text-slate-400">{subtitle}</Text>
      ) : null}
    </View>
  );
}
