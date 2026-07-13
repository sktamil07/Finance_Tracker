import React, { useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import {
  useBudgetGoals,
  useCategories,
  useCreateBudgetGoal,
  useDeleteBudgetGoal,
  useUpdateBudgetGoal,
} from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useCurrency } from '@/auth/AuthContext';
import { Fab, FormModal } from '@/components/FormModal';
import {
  MonthField,
  SelectField,
  SwitchField,
  TextField,
  type SelectOption,
} from '@/components/form';
import { Badge, Button, Card } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { resolveCategoryIcon } from '@/lib/icons';
import { formatMoney } from '@/lib/money';
import { currentYearMonth, formatYearMonthLong } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';
import type { BudgetGoalRequest, BudgetGoalResponse } from '@/types/api';

const schema = z.object({
  categoryId: z.number().refine((v) => v > 0, 'Select a category'),
  limitAmount: z
    .string()
    .min(1, 'Limit is required')
    .refine((v) => Number(v) > 0, 'Enter a limit greater than 0'),
  recurring: z.boolean(),
  month: z.string().min(1),
});
type FormValues = z.infer<typeof schema>;

function toDefaults(goal?: BudgetGoalResponse): FormValues {
  return {
    categoryId: goal?.categoryId ?? 0,
    limitAmount: goal ? String(goal.limitAmount) : '',
    recurring: goal ? goal.recurring : true,
    month: goal?.month ?? currentYearMonth(),
  };
}

function BudgetGoalForm({ editing, onClose }: { editing?: BudgetGoalResponse; onClose: () => void }) {
  const categories = useCategories('EXPENSE');
  const create = useCreateBudgetGoal();
  const update = useUpdateBudgetGoal();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, watch, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(editing),
  });
  const recurring = watch('recurring');

  const options: SelectOption[] = useMemo(
    () =>
      (categories.data ?? []).map((c) => ({
        label: c.name,
        value: c.id,
        icon: resolveCategoryIcon(c.icon),
      })),
    [categories.data],
  );

  const onSubmit = handleSubmit(async (v) => {
    setSubmitError(null);
    const payload: BudgetGoalRequest = {
      categoryId: v.categoryId,
      limitAmount: Number(v.limitAmount),
      month: v.recurring ? null : v.month,
    };
    try {
      if (editing) await update.mutateAsync({ id: editing.id, body: payload });
      else await create.mutateAsync(payload);
      onClose();
    } catch (err) {
      setSubmitError(getApiErrorMessage(err));
    }
  });

  return (
    <View>
      <SelectField
        control={control}
        name="categoryId"
        label="Category"
        options={options}
        placeholder={categories.isLoading ? 'Loading…' : 'Select a category'}
      />
      <TextField control={control} name="limitAmount" label="Monthly limit" placeholder="0" keyboardType="decimal-pad" />
      <SwitchField
        control={control}
        name="recurring"
        label="Recurring goal"
        hint="Applies to every month unless a month-specific goal overrides it"
      />
      {recurring ? null : <MonthField control={control} name="month" label="Month" />}

      {submitError ? (
        <Text className="mb-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <Button
        label={editing ? 'Save changes' : 'Add budget goal'}
        onPress={onSubmit}
        loading={formState.isSubmitting}
        fullWidth
      />
    </View>
  );
}

export function BudgetGoalsScreen() {
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<BudgetGoalResponse | undefined>(undefined);
  const currency = useCurrency();
  const { palette } = useTheme();

  const goals = useBudgetGoals();
  const del = useDeleteBudgetGoal();

  const openAdd = () => {
    setEditing(undefined);
    setModalOpen(true);
  };
  const openEdit = (g: BudgetGoalResponse) => {
    setEditing(g);
    setModalOpen(true);
  };
  const confirmDelete = (g: BudgetGoalResponse) => {
    Alert.alert('Delete budget goal', `Delete the goal for “${g.categoryName}”?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => del.mutate(g.id, { onError: (e) => Alert.alert('Error', getApiErrorMessage(e)) }),
      },
    ]);
  };

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      {goals.isLoading ? (
        <LoadingState label="Loading budget goals…" />
      ) : goals.isError ? (
        <ErrorState message={getApiErrorMessage(goals.error)} onRetry={() => goals.refetch()} />
      ) : (
        <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 96 }}>
          {(goals.data?.length ?? 0) === 0 ? (
            <Card>
              <EmptyState
                icon="target"
                title="No budget goals yet"
                subtitle="Tap + to set a monthly spending limit for a category."
              />
            </Card>
          ) : (
            goals.data!.map((g) => (
              <Card key={g.id} className="mb-2">
                <View className="flex-row items-center">
                  <Pressable className="flex-1" onPress={() => openEdit(g)}>
                    <View className="flex-row items-center justify-between">
                      <Text className="font-semibold text-gray-900 dark:text-white">
                        {g.categoryName}
                      </Text>
                      <Text className="font-bold text-gray-900 dark:text-white">
                        {formatMoney(g.limitAmount, currency)}
                      </Text>
                    </View>
                    <View className="mt-1">
                      {g.recurring ? (
                        <Badge label="Every month" tone="brand" />
                      ) : (
                        <Badge label={g.month ? formatYearMonthLong(g.month) : 'One month'} tone="neutral" />
                      )}
                    </View>
                  </Pressable>
                  <Pressable
                    onPress={() => confirmDelete(g)}
                    hitSlop={8}
                    accessibilityLabel="Delete budget goal"
                    className="ml-2 h-9 w-9 items-center justify-center rounded-full active:bg-red-50 dark:active:bg-red-900/30"
                  >
                    <MaterialCommunityIcons name="trash-can-outline" size={20} color={palette.danger} />
                  </Pressable>
                </View>
              </Card>
            ))
          )}
        </ScrollView>
      )}

      <Fab onPress={openAdd} label="Add budget goal" />

      <FormModal
        visible={modalOpen}
        title={editing ? 'Edit budget goal' : 'New budget goal'}
        onClose={() => setModalOpen(false)}
      >
        {modalOpen ? <BudgetGoalForm editing={editing} onClose={() => setModalOpen(false)} /> : null}
      </FormModal>
    </View>
  );
}
