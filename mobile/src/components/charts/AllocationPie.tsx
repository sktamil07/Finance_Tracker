import React from 'react';
import { Text, View } from 'react-native';
import Svg, { Circle, G } from 'react-native-svg';

import { useTheme } from '@/theme/ThemeProvider';

export interface PieSegment {
  label: string;
  value: number; // absolute amount; share is computed from the total
  color: string;
}

interface Props {
  segments: PieSegment[];
  size?: number;
  strokeWidth?: number;
  centerTitle?: string;
  centerSubtitle?: string;
}

/**
 * Donut allocation chart, drawn with react-native-svg (no native chart module,
 * so it runs in Expo Go on both platforms). Negative segments (e.g. savings
 * when the user overspent) are clamped to 0 for the arc — the breakdown list
 * still shows the real signed number.
 */
export function AllocationPie({
  segments,
  size = 180,
  strokeWidth = 26,
  centerTitle,
  centerSubtitle,
}: Props) {
  const { palette } = useTheme();
  const radius = (size - strokeWidth) / 2;
  const cx = size / 2;
  const cy = size / 2;

  const positive = segments.map((s) => ({ ...s, value: Math.max(0, s.value) }));
  const total = positive.reduce((sum, s) => sum + s.value, 0);

  // Nothing to draw → show a neutral ring so the card isn't empty.
  if (total <= 0) {
    return (
      <View style={{ width: size, height: size }} className="items-center justify-center">
        <Svg width={size} height={size}>
          <Circle
            cx={cx}
            cy={cy}
            r={radius}
            stroke={palette.border}
            strokeWidth={strokeWidth}
            fill="none"
          />
        </Svg>
        <View className="absolute items-center">
          <Text className="text-xs text-gray-500 dark:text-slate-400">No data</Text>
        </View>
      </View>
    );
  }

  const circumference = 2 * Math.PI * radius;
  let offset = 0;
  const arcs = positive
    .filter((s) => s.value > 0)
    .map((s) => {
      const fraction = s.value / total;
      const dash = fraction * circumference;
      const arc = (
        <Circle
          key={s.label}
          cx={cx}
          cy={cy}
          r={radius}
          stroke={s.color}
          strokeWidth={strokeWidth}
          strokeDasharray={`${dash} ${circumference - dash}`}
          strokeDashoffset={-offset}
          strokeLinecap="butt"
          fill="none"
        />
      );
      offset += dash;
      return arc;
    });

  return (
    <View style={{ width: size, height: size }} className="items-center justify-center">
      <Svg width={size} height={size}>
        {/* track */}
        <Circle
          cx={cx}
          cy={cy}
          r={radius}
          stroke={palette.border}
          strokeWidth={strokeWidth}
          fill="none"
          opacity={0.35}
        />
        {/* rotate -90° so the first slice starts at 12 o'clock */}
        <G rotation={-90} origin={`${cx}, ${cy}`}>
          {arcs}
        </G>
      </Svg>
      {(centerTitle || centerSubtitle) && (
        <View className="absolute items-center">
          {centerSubtitle ? (
            <Text className="text-[11px] font-medium uppercase tracking-wide text-gray-500 dark:text-slate-400">
              {centerSubtitle}
            </Text>
          ) : null}
          {centerTitle ? (
            <Text className="text-lg font-bold text-gray-900 dark:text-white">{centerTitle}</Text>
          ) : null}
        </View>
      )}
    </View>
  );
}
