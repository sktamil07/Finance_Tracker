import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  Text,
  View,
  type PressableProps,
} from 'react-native';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';

const VARIANT_CLASSES: Record<ButtonVariant, { container: string; text: string }> = {
  primary: { container: 'bg-brand-600 active:bg-brand-700', text: 'text-white' },
  secondary: {
    container: 'bg-gray-200 active:bg-gray-300 dark:bg-slate-700 dark:active:bg-slate-600',
    text: 'text-gray-900 dark:text-white',
  },
  danger: { container: 'bg-red-600 active:bg-red-700', text: 'text-white' },
  ghost: { container: 'bg-transparent active:bg-gray-100 dark:active:bg-slate-800', text: 'text-brand-600 dark:text-brand-500' },
};

export function Button({
  label,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  fullWidth = false,
  ...rest
}: {
  label: string;
  onPress?: () => void;
  variant?: ButtonVariant;
  loading?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
} & Omit<PressableProps, 'onPress'>) {
  const v = VARIANT_CLASSES[variant];
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={isDisabled}
      className={`h-12 flex-row items-center justify-center rounded-xl px-5 ${v.container} ${
        isDisabled ? 'opacity-50' : ''
      } ${fullWidth ? 'w-full' : ''}`}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'secondary' ? '#6B7280' : '#FFFFFF'} />
      ) : (
        <Text className={`text-base font-semibold ${v.text}`}>{label}</Text>
      )}
    </Pressable>
  );
}

/** A rounded, elevated surface used across every screen. */
export function Card({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <View
      className={`rounded-2xl border border-gray-200 bg-white p-4 dark:border-slate-700/60 dark:bg-[#141C2B] ${className}`}
    >
      {children}
    </View>
  );
}

export function SectionHeader({
  title,
  action,
}: {
  title: string;
  action?: React.ReactNode;
}) {
  return (
    <View className="mb-2 mt-4 flex-row items-center justify-between">
      <Text className="text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-slate-400">
        {title}
      </Text>
      {action}
    </View>
  );
}

type BadgeTone = 'neutral' | 'positive' | 'warning' | 'danger' | 'brand';

const BADGE_CLASSES: Record<BadgeTone, { container: string; text: string }> = {
  neutral: {
    container: 'bg-gray-100 dark:bg-slate-700',
    text: 'text-gray-700 dark:text-slate-200',
  },
  positive: {
    container: 'bg-green-100 dark:bg-green-900/40',
    text: 'text-green-700 dark:text-green-300',
  },
  warning: {
    container: 'bg-amber-100 dark:bg-amber-900/40',
    text: 'text-amber-700 dark:text-amber-300',
  },
  danger: {
    container: 'bg-red-100 dark:bg-red-900/40',
    text: 'text-red-700 dark:text-red-300',
  },
  brand: {
    container: 'bg-brand-100 dark:bg-brand-900/40',
    text: 'text-brand-700 dark:text-brand-500',
  },
};

export function Badge({ label, tone = 'neutral' }: { label: string; tone?: BadgeTone }) {
  const { container, text } = BADGE_CLASSES[tone];
  return (
    <View className={`self-start rounded-full px-2 py-0.5 ${container}`}>
      <Text className={`text-xs font-semibold ${text}`}>{label}</Text>
    </View>
  );
}

export interface SegmentOption<T extends string | number> {
  label: string;
  value: T;
}

/** iOS-style segmented control used for small mutually-exclusive choices. */
export function Segmented<T extends string | number>({
  options,
  value,
  onChange,
}: {
  options: SegmentOption<T>[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View className="flex-row rounded-xl bg-gray-200 p-1 dark:bg-slate-800">
      {options.map((opt) => {
        const active = opt.value === value;
        return (
          <Pressable
            key={String(opt.value)}
            onPress={() => onChange(opt.value)}
            className={`flex-1 items-center justify-center rounded-lg py-2 ${
              active ? 'bg-white dark:bg-slate-600' : ''
            }`}
          >
            <Text
              className={`text-sm font-semibold ${
                active ? 'text-brand-600 dark:text-white' : 'text-gray-500 dark:text-slate-400'
              }`}
            >
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}
