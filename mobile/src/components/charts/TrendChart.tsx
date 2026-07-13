import React from 'react';
import { Text, View } from 'react-native';
import Svg, { Circle, Line, Polyline, Text as SvgText } from 'react-native-svg';

import { useTheme } from '@/theme/ThemeProvider';

export interface TrendPoint {
  label: string;
  value: number;
}

/**
 * Minimal responsive line chart drawn with react-native-svg (Expo Go safe, no
 * native chart module). Y is auto-scaled to the data with a little headroom; the
 * first and last x labels are drawn to keep the axis uncluttered on narrow
 * screens.
 */
export function TrendChart({
  points,
  height = 160,
  width = 320,
  suffix = '',
}: {
  points: TrendPoint[];
  height?: number;
  width?: number;
  suffix?: string;
}) {
  const { palette } = useTheme();

  if (points.length < 2) {
    return (
      <View style={{ height }} className="items-center justify-center">
        <Text className="text-sm text-gray-500 dark:text-slate-400">Not enough data to chart</Text>
      </View>
    );
  }

  const padX = 8;
  const padTop = 12;
  const padBottom = 22;
  const plotW = width - padX * 2;
  const plotH = height - padTop - padBottom;

  const values = points.map((p) => p.value);
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const span = rawMax - rawMin || 1;
  const min = rawMin - span * 0.1;
  const max = rawMax + span * 0.1;

  const x = (i: number) => padX + (i / (points.length - 1)) * plotW;
  const y = (v: number) => padTop + (1 - (v - min) / (max - min)) * plotH;

  const polyline = points.map((p, i) => `${x(i)},${y(p.value)}`).join(' ');
  const baseline = padTop + plotH;

  return (
    <View>
      <Svg width={width} height={height}>
        {/* baseline */}
        <Line x1={padX} y1={baseline} x2={width - padX} y2={baseline} stroke={palette.border} strokeWidth={1} />
        <Polyline
          points={polyline}
          fill="none"
          stroke={palette.brand}
          strokeWidth={2.5}
          strokeLinejoin="round"
          strokeLinecap="round"
        />
        {points.map((p, i) => (
          <Circle key={i} cx={x(i)} cy={y(p.value)} r={3} fill={palette.brand} />
        ))}
        {/* first + last x labels */}
        <SvgText x={padX} y={height - 6} fontSize={10} fill={palette.textMuted} textAnchor="start">
          {points[0].label}
        </SvgText>
        <SvgText
          x={width - padX}
          y={height - 6}
          fontSize={10}
          fill={palette.textMuted}
          textAnchor="end"
        >
          {points[points.length - 1].label}
        </SvgText>
      </Svg>
      <View className="mt-1 flex-row justify-between">
        <Text className="text-xs text-gray-500 dark:text-slate-400">
          low {rawMin.toFixed(1)}
          {suffix}
        </Text>
        <Text className="text-xs text-gray-500 dark:text-slate-400">
          high {rawMax.toFixed(1)}
          {suffix}
        </Text>
      </View>
    </View>
  );
}
