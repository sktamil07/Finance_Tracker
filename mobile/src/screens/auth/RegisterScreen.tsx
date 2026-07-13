import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useAuth } from '@/auth/AuthContext';
import { getApiErrorMessage } from '@/api/errors';
import { Button } from '@/components/ui';
import { TextField } from '@/components/form';
import { Screen } from '@/components/Screen';
import type { AuthStackParamList } from '@/navigation/types';

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(120, 'Name is too long'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(72, 'Password is too long'),
});
type FormValues = z.infer<typeof schema>;

export function RegisterScreen() {
  const { register } = useAuth();
  const navigation = useNavigation<NativeStackNavigationProp<AuthStackParamList>>();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', email: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      await register(values);
    } catch (err) {
      setSubmitError(getApiErrorMessage(err, 'Registration failed.'));
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
          <View className="mb-8">
            <Text className="text-2xl font-bold text-gray-900 dark:text-white">Create account</Text>
            <Text className="mt-1 text-gray-500 dark:text-slate-400">
              Start tracking your finances
            </Text>
          </View>

          <TextField
            control={control}
            name="name"
            label="Name"
            placeholder="Your name"
            autoCapitalize="words"
          />
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
            placeholder="At least 8 characters"
            secureTextEntry
            autoCapitalize="none"
          />

          {submitError ? (
            <Text className="mb-3 text-center text-sm text-red-600 dark:text-red-400">
              {submitError}
            </Text>
          ) : null}

          <Button
            label="Create account"
            onPress={onSubmit}
            loading={formState.isSubmitting}
            fullWidth
          />

          <View className="mt-6 flex-row justify-center">
            <Text className="text-gray-500 dark:text-slate-400">Already have an account? </Text>
            <Text
              onPress={() => navigation.navigate('Login')}
              className="font-semibold text-brand-600 dark:text-brand-500"
            >
              Sign in
            </Text>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
