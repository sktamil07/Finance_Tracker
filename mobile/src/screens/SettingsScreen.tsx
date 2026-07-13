import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useSettings, useUpdateSettings } from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useAuth } from '@/auth/AuthContext';
import { SelectField, TextField, type SelectOption } from '@/components/form';
import { Button, Card, SectionHeader, Segmented } from '@/components/ui';
import { ErrorState, LoadingState } from '@/components/states';
import { useTheme } from '@/theme/ThemeProvider';
import type { AppStackParamList } from '@/navigation/types';
import type { SettingsResponse, ThemePreference, UpdateSettingsRequest } from '@/types/api';

const CURRENCIES: SelectOption[] = [
  { label: 'Indian Rupee (₹ INR)', value: 'INR' },
  { label: 'US Dollar ($ USD)', value: 'USD' },
  { label: 'Euro (€ EUR)', value: 'EUR' },
  { label: 'British Pound (£ GBP)', value: 'GBP' },
  { label: 'Japanese Yen (¥ JPY)', value: 'JPY' },
  { label: 'Australian Dollar (A$ AUD)', value: 'AUD' },
  { label: 'Canadian Dollar (C$ CAD)', value: 'CAD' },
  { label: 'Singapore Dollar (S$ SGD)', value: 'SGD' },
  { label: 'UAE Dirham (AED)', value: 'AED' },
];

const schema = z.object({
  displayName: z.string().min(1, 'Display name is required').max(120, 'Too long'),
  currencyCode: z.string().regex(/^[A-Z]{3}$/, 'Pick a currency'),
});
type FormValues = z.infer<typeof schema>;

function NavRow({
  icon,
  label,
  onPress,
}: {
  icon: React.ComponentProps<typeof MaterialCommunityIcons>['name'];
  label: string;
  onPress: () => void;
}) {
  const { palette } = useTheme();
  return (
    <Pressable
      onPress={onPress}
      className="flex-row items-center border-b border-gray-100 py-3.5 last:border-0 dark:border-slate-700/60"
    >
      <MaterialCommunityIcons name={icon} size={22} color={palette.brand} />
      <Text className="ml-3 flex-1 text-base text-gray-900 dark:text-white">{label}</Text>
      <MaterialCommunityIcons name="chevron-right" size={22} color={palette.textMuted} />
    </Pressable>
  );
}

function SettingsForm({ settings }: { settings: SettingsResponse }) {
  const { setPreference } = useTheme();
  const { applySettings } = useAuth();
  const update = useUpdateSettings();
  const [theme, setTheme] = useState<ThemePreference>(settings.themePreference);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      displayName: settings.displayName,
      currencyCode: settings.currencyCode,
    },
  });

  const onThemeChange = (pref: ThemePreference) => {
    setTheme(pref);
    setPreference(pref); // live preview
  };

  const onSubmit = handleSubmit(async (v) => {
    setSubmitError(null);
    const payload: UpdateSettingsRequest = {
      displayName: v.displayName.trim(),
      currencyCode: v.currencyCode,
      themePreference: theme,
    };
    try {
      const saved = await update.mutateAsync(payload);
      applySettings(saved);
      Alert.alert('Saved', 'Your settings have been updated.');
    } catch (err) {
      setSubmitError(getApiErrorMessage(err));
    }
  });

  return (
    <>
      <SectionHeader title="Profile" />
      <Card>
        <TextField control={control} name="displayName" label="Display name" autoCapitalize="words" />
        <View className="mb-2">
          <Text className="mb-1.5 text-sm font-medium text-gray-700 dark:text-slate-300">Email</Text>
          <Text className="text-base text-gray-500 dark:text-slate-400">{settings.email}</Text>
        </View>
      </Card>

      <SectionHeader title="Preferences" />
      <Card>
        <SelectField control={control} name="currencyCode" label="Currency" options={CURRENCIES} />
        <Text className="mb-1.5 text-sm font-medium text-gray-700 dark:text-slate-300">Theme</Text>
        <Segmented
          value={theme}
          onChange={onThemeChange}
          options={[
            { label: 'Light', value: 'LIGHT' },
            { label: 'Dark', value: 'DARK' },
            { label: 'System', value: 'SYSTEM' },
          ]}
        />
      </Card>

      {submitError ? (
        <Text className="mt-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <View className="mt-4">
        <Button label="Save changes" onPress={onSubmit} loading={formState.isSubmitting} fullWidth />
      </View>
    </>
  );
}

export function SettingsScreen() {
  const settings = useSettings();
  const { logout } = useAuth();
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();

  const confirmLogout = () => {
    Alert.alert('Log out', 'Are you sure you want to log out?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Log out', style: 'destructive', onPress: () => logout() },
    ]);
  };

  return (
    <ScrollView className="flex-1 bg-gray-100 dark:bg-[#0B1220]" contentContainerStyle={{ padding: 16 }}>
      {settings.isLoading ? (
        <LoadingState label="Loading settings…" />
      ) : settings.isError ? (
        <ErrorState message={getApiErrorMessage(settings.error)} onRetry={() => settings.refetch()} />
      ) : settings.data ? (
        <SettingsForm settings={settings.data} />
      ) : null}

      <SectionHeader title="Manage" />
      <Card>
        <NavRow icon="cash-plus" label="Income" onPress={() => navigation.navigate('Income')} />
        <NavRow icon="shape-outline" label="Categories" onPress={() => navigation.navigate('Categories')} />
        <NavRow icon="target" label="Budget goals" onPress={() => navigation.navigate('BudgetGoals')} />
        <NavRow icon="chart-timeline-variant" label="Reports" onPress={() => navigation.navigate('Reports')} />
      </Card>

      <View className="mt-6">
        <Button label="Log out" onPress={confirmLogout} variant="danger" fullWidth />
      </View>
    </ScrollView>
  );
}
