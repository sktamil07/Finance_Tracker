import React, { useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import {
  useCreateIncome,
  useDeleteIncome,
  useIncome,
  useUpdateIncome,
} from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useCurrency } from '@/auth/AuthContext';
import { Fab, FormModal } from '@/components/FormModal';
import { MonthField, TextField } from '@/components/form';
import { MonthSelector } from '@/components/MonthSelector';
import { Button, Card, SectionHeader } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { formatMoney } from '@/lib/money';
import { currentYearMonth, formatLocalDate } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';
import type { IncomeRequest, IncomeResponse } from '@/types/api';

const schema = z.object({
  amount: z
    .string()
    .min(1, 'Amount is required')
    .refine((v) => Number(v) > 0, 'Enter an amount greater than 0'),
  source: z.string().max(100, 'Too long').optional(),
  month: z.string().min(1),
});
type FormValues = z.infer<typeof schema>;

function toDefaults(month: string, inc?: IncomeResponse): FormValues {
  return {
    amount: inc ? String(inc.amount) : '',
    source: inc?.source ?? 'Salary',
    month: inc?.month ?? month,
  };
}

function IncomeForm({
  month,
  editing,
  onClose,
}: {
  month: string;
  editing?: IncomeResponse;
  onClose: () => void;
}) {
  const create = useCreateIncome();
  const update = useUpdateIncome();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(month, editing),
  });

  const onSubmit = handleSubmit(async (v) => {
    setSubmitError(null);
    const body: IncomeRequest = {
      month: v.month,
      amount: Number(v.amount),
      source: v.source?.trim() ? v.source.trim() : 'Salary',
    };
    try {
      if (editing) await update.mutateAsync({ id: editing.id, body });
      else await create.mutateAsync(body);
      onClose();
    } catch (err) {
      setSubmitError(getApiErrorMessage(err));
    }
  });

  return (
    <View>
      <TextField control={control} name="amount" label="Amount" placeholder="e.g. 191000" keyboardType="decimal-pad" />
      <TextField control={control} name="source" label="Source" placeholder="Salary" />
      <MonthField control={control} name="month" label="Month" />
      {submitError ? (
        <Text className="mb-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <Button
        label={editing ? 'Save changes' : 'Add income'}
        onPress={onSubmit}
        loading={formState.isSubmitting}
        fullWidth
      />
    </View>
  );
}

function IncomeRow({
  inc,
  currency,
  onEdit,
  onDelete,
}: {
  inc: IncomeResponse;
  currency: string;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { palette } = useTheme();
  return (
    <Card className="mb-2">
      <View className="flex-row items-center">
        <View
          style={{ backgroundColor: `${palette.positive}22` }}
          className="mr-3 h-10 w-10 items-center justify-center rounded-full"
        >
          <MaterialCommunityIcons name="cash-plus" size={20} color={palette.positive} />
        </View>
        <Pressable className="flex-1" onPress={onEdit}>
          <View className="flex-row items-center justify-between">
            <Text className="font-semibold text-gray-900 dark:text-white" numberOfLines={1}>
              {inc.source}
            </Text>
            <Text className="font-bold text-gray-900 dark:text-white">
              {formatMoney(inc.amount, currency)}
            </Text>
          </View>
          <Text className="text-xs text-gray-500 dark:text-slate-400">
            Added {formatLocalDate(inc.createdAt.slice(0, 10))}
          </Text>
        </Pressable>
        <Pressable
          onPress={onDelete}
          hitSlop={10}
          accessibilityLabel="Delete income"
          className="ml-2 h-10 w-10 items-center justify-center rounded-full active:bg-red-50 dark:active:bg-red-900/30"
        >
          <MaterialCommunityIcons name="trash-can-outline" size={20} color={palette.danger} />
        </Pressable>
      </View>
    </Card>
  );
}

export function IncomeScreen() {
  const [month, setMonth] = useState(currentYearMonth());
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<IncomeResponse | undefined>(undefined);
  const currency = useCurrency();

  const list = useIncome(month);
  const del = useDeleteIncome();

  const entries = list.data ?? [];
  const total = entries.reduce((sum, e) => sum + e.amount, 0);

  const openAdd = () => {
    setEditing(undefined);
    setModalOpen(true);
  };
  const openEdit = (inc: IncomeResponse) => {
    setEditing(inc);
    setModalOpen(true);
  };
  const confirmDelete = (inc: IncomeResponse) => {
    Alert.alert('Delete income', `Delete “${inc.source}” (${formatMoney(inc.amount, currency)})?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => del.mutate(inc.id, { onError: (e) => Alert.alert('Error', getApiErrorMessage(e)) }),
      },
    ]);
  };

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="px-4 pt-3">
        <MonthSelector month={month} onChange={setMonth} />
      </View>

      {list.isLoading ? (
        <LoadingState label="Loading income…" />
      ) : list.isError ? (
        <ErrorState message={getApiErrorMessage(list.error)} onRetry={() => list.refetch()} />
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingBottom: 96 }}
          refreshControl={<RefreshControl refreshing={list.isRefetching} onRefresh={() => list.refetch()} />}
        >
          <Card className="mb-3 flex-row items-center justify-between">
            <Text className="text-sm font-medium text-gray-500 dark:text-slate-400">
              Total this month
            </Text>
            <Text className="text-xl font-bold text-gray-900 dark:text-white">
              {formatMoney(total, currency)}
            </Text>
          </Card>

          <SectionHeader title="Income entries" />
          {entries.length === 0 ? (
            <Card>
              <EmptyState
                icon="cash-plus"
                title="No income yet"
                subtitle="Tap + to record your salary or other income for this month."
              />
            </Card>
          ) : (
            entries.map((inc) => (
              <IncomeRow
                key={inc.id}
                inc={inc}
                currency={currency}
                onEdit={() => openEdit(inc)}
                onDelete={() => confirmDelete(inc)}
              />
            ))
          )}
        </ScrollView>
      )}

      <Fab onPress={openAdd} label="Add income" />

      <FormModal
        visible={modalOpen}
        title={editing ? 'Edit income' : 'Add income'}
        onClose={() => setModalOpen(false)}
      >
        {modalOpen ? <IncomeForm month={month} editing={editing} onClose={() => setModalOpen(false)} /> : null}
      </FormModal>
    </View>
  );
}
