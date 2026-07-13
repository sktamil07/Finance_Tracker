import React from 'react';
import { View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { resolveCategoryIcon } from '@/lib/icons';

/** A category's icon inside a soft tinted circle. `color` tints both. */
export function CategoryIcon({
  slug,
  color,
  size = 20,
}: {
  slug: string | null | undefined;
  color: string;
  size?: number;
}) {
  const box = size + 18;
  return (
    <View
      style={{ width: box, height: box, backgroundColor: `${color}22` }}
      className="items-center justify-center rounded-full"
    >
      <MaterialCommunityIcons name={resolveCategoryIcon(slug)} size={size} color={color} />
    </View>
  );
}
