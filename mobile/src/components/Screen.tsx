import React from 'react';
import { View } from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';

/**
 * Standard screen wrapper: safe-area aware, themed background. `edges` defaults
 * to top only, since the tab bar already handles the bottom inset.
 */
export function Screen({
  children,
  edges = ['top'],
  className = '',
}: {
  children: React.ReactNode;
  edges?: Edge[];
  className?: string;
}) {
  return (
    <SafeAreaView edges={edges} className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className={`flex-1 ${className}`}>{children}</View>
    </SafeAreaView>
  );
}
