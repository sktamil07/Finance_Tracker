import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useAuth } from '@/auth/AuthContext';
import { getApiErrorMessage } from '@/api/errors';
import { Button } from '@/components/ui';
import { TextField } from '@/components/form';
import { Screen } from '@/components/Screen';
import { useTheme } from '@/theme/ThemeProvider';
import type { AuthStackParamList } from '@/navigation/types';

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});
type FormValues = z.infer<typeof schema>;

export function LoginScreen() {
  const { login } = useAuth();
  const { palette } = useTheme();
  const navigation = useNavigation<NativeStackNavigationProp<AuthStackParamList>>();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await login(values);
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, 'Login failed. Check your credentials.'));
    }
  });

  return (
    <Screen edges={['top', 'bottom']}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        className="flex-1"
      >
        <ScrollView
          contentContainerStyle={{ flexGrow: 1, justifyContent: 'center', padding: 24 }}
          keyboardShouldPersistTaps="handled"
        >
          <View className="mb-8 items-center">
            <View
              style={{ backgroundColor: `${palette.brand}22` }}
              className="mb-4 h-16 w-16 items-center justify-center rounded-2xl"
            >
              <MaterialCommunityIcons name="finance" size={34} color={palette.brand} />
            </View>
            <Text className="text-2xl font-bold text-gray-900 dark:text-white">Finance Tracker</Text>
            <Text className="mt-1 text-gray-500 dark:text-slate-400">Sign in to your account</Text>
          </View>

          <TextField
            control={control}
            name="email"
            label="Email"
            placeholder="you@example.com"
            keyboardType="email-address"
            autoCapitalize="none"
          />
          <TextField
            control={control}
            name="password"
            label="Password"
            placeholder="••••••••"
            secureTextEntry
            autoCapitalize="none"
          />

          {submitError ? (
            <Text className="mb-3 text-center text-sm text-red-600 dark:text-red-400">
              {submitError}
            </Text>
          ) : null}

          <Button
            label="Sign in"
            onPress={onSubmit}
            loading={formState.isSubmitting}
            fullWidth
          />

          <View className="mt-6 flex-row justify-center">
            <Text className="text-gray-500 dark:text-slate-400">No account? </Text>
            <Text
              onPress={() => navigation.navigate('Register')}
              className="font-semibold text-brand-600 dark:text-brand-500"
            >
              Create one
            </Text>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
