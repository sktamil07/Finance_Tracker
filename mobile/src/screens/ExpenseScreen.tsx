import React, { useMemo, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import {
  useBudgets,
  useCategories,
  useCreateExpense,
  useDeleteExpense,
  useExpensesGrouped,
  useUpdateExpense,
} from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useCurrency } from '@/auth/AuthContext';
import { CategoryIcon } from '@/components/CategoryIcon';
import { Fab, FormModal } from '@/components/FormModal';
import { DateField, SelectField, TextField, type SelectOption } from '@/components/form';
import { MonthSelector } from '@/components/MonthSelector';
import { Badge, Button, Card, SectionHeader } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { resolveCategoryIcon } from '@/lib/icons';
import { formatMoney, formatPercent } from '@/lib/money';
import { currentYearMonth, formatLocalDate, todayLocalDate } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';
import type {
  BudgetStatus,
  ExpenseCategoryGroup,
  ExpenseRequest,
  ExpenseResponse,
} from '@/types/api';

const schema = z.object({
  description: z.string().min(1, 'Description is required').max(200, 'Too long'),
  amount: z
    .string()
    .min(1, 'Amount is required')
    .refine((v) => Number(v) > 0, 'Enter an amount greater than 0'),
  categoryId: z.number().refine((v) => v > 0, 'Select a category'),
  transactionDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD'),
  notes: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

function toDefaults(exp?: ExpenseResponse): FormValues {
  return {
    description: exp?.description ?? '',
    amount: exp ? String(exp.amount) : '',
    categoryId: exp?.categoryId ?? 0,
    transactionDate: exp?.transactionDate ?? todayLocalDate(),
    notes: exp?.notes ?? '',
  };
}

function ExpenseForm({ editing, onClose }: { editing?: ExpenseResponse; onClose: () => void }) {
  const categories = useCategories('EXPENSE');
  const create = useCreateExpense();
  const update = useUpdateExpense();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(editing),
  });

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
    const payload: ExpenseRequest = {
      description: v.description.trim(),
      amount: Number(v.amount),
      categoryId: v.categoryId,
      transactionDate: v.transactionDate,
      notes: v.notes?.trim() ? v.notes.trim() : null,
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
      <TextField control={control} name="description" label="Description" placeholder="e.g. Groceries" />
      <TextField control={control} name="amount" label="Amount" placeholder="0" keyboardType="decimal-pad" />
      <SelectField
        control={control}
        name="categoryId"
        label="Category"
        options={options}
        placeholder={categories.isLoading ? 'Loading…' : 'Select a category'}
      />
      <DateField control={control} name="transactionDate" label="Date" />
      <TextField control={control} name="notes" label="Notes (optional)" placeholder="Anything to remember" multiline />

      {submitError ? (
        <Text className="mb-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <Button
        label={editing ? 'Save changes' : 'Add expense'}
        onPress={onSubmit}
        loading={formState.isSubmitting}
        fullWidth
      />
    </View>
  );
}

function BudgetBadge({ status, pct }: { status: BudgetStatus; pct: number }) {
  if (status === 'EXCEEDED') return <Badge label={`Over budget · ${formatPercent(pct, 0)}`} tone="danger" />;
  if (status === 'WARNING') return <Badge label={`Budget ${formatPercent(pct, 0)}`} tone="warning" />;
  return null;
}

function CategoryGroup({
  group,
  currency,
  budget,
  onEditTx,
  onDeleteTx,
}: {
  group: ExpenseCategoryGroup;
  currency: string;
  budget?: { status: BudgetStatus; pct: number };
  onEditTx: (t: ExpenseResponse) => void;
  onDeleteTx: (t: ExpenseResponse) => void;
}) {
  const { palette } = useTheme();
  const [expanded, setExpanded] = useState(true);

  return (
    <Card className="mb-3">
      <Pressable onPress={() => setExpanded((e) => !e)} className="flex-row items-center">
        <CategoryIcon slug={group.categoryIcon} color={palette.categorical[0]} />
        <View className="ml-3 flex-1">
          <View className="flex-row items-center justify-between">
            <Text className="font-semibold text-gray-900 dark:text-white" numberOfLines={1}>
              {group.categoryName}
            </Text>
            <Text className="font-bold text-gray-900 dark:text-white">
              {formatMoney(group.total, currency)}
            </Text>
          </View>
          <View className="mt-1 flex-row items-center justify-between">
            <View className="flex-row gap-2">
              <Text className="text-xs text-gray-500 dark:text-slate-400">
                {formatPercent(group.percentageOfMonth)} of month
              </Text>
              {budget ? <BudgetBadge status={budget.status} pct={budget.pct} /> : null}
            </View>
            <MaterialCommunityIcons
              name={expanded ? 'chevron-up' : 'chevron-down'}
              size={18}
              color={palette.textMuted}
            />
          </View>
        </View>
      </Pressable>

      {expanded ? (
        <View className="mt-3 border-t border-gray-100 pt-1 dark:border-slate-700/60">
          {group.transactions.map((t) => (
            <View
              key={t.id}
              className="flex-row items-center border-b border-gray-50 py-2 dark:border-slate-800"
            >
              <Pressable className="flex-1" onPress={() => onEditTx(t)}>
                <Text className="text-gray-900 dark:text-white" numberOfLines={1}>
                  {t.description}
                </Text>
                <Text className="text-xs text-gray-500 dark:text-slate-400">
                  {formatLocalDate(t.transactionDate)}
                  {t.notes ? ` · ${t.notes}` : ''}
                </Text>
              </Pressable>
              <Text className="mr-2 font-semibold text-gray-900 dark:text-white">
                {formatMoney(t.amount, currency)}
              </Text>
              <Pressable
                onPress={() => onDeleteTx(t)}
                hitSlop={8}
                accessibilityLabel="Delete expense"
                className="h-8 w-8 items-center justify-center rounded-full active:bg-red-50 dark:active:bg-red-900/30"
              >
                <MaterialCommunityIcons name="trash-can-outline" size={18} color={palette.danger} />
              </Pressable>
            </View>
          ))}
        </View>
      ) : null}
    </Card>
  );
}

export function ExpenseScreen() {
  const [month, setMonth] = useState(currentYearMonth());
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ExpenseResponse | undefined>(undefined);
  const currency = useCurrency();

  const grouped = useExpensesGrouped(month);
  const budgets = useBudgets(month);
  const del = useDeleteExpense();

  const budgetByCategory = useMemo(() => {
    const map = new Map<number, { status: BudgetStatus; pct: number }>();
    for (const b of budgets.data ?? []) {
      map.set(b.categoryId, { status: b.status, pct: b.utilisationPercentage });
    }
    return map;
  }, [budgets.data]);

  const openAdd = () => {
    setEditing(undefined);
    setModalOpen(true);
  };
  const openEdit = (t: ExpenseResponse) => {
    setEditing(t);
    setModalOpen(true);
  };
  const confirmDelete = (t: ExpenseResponse) => {
    Alert.alert('Delete expense', `Delete “${t.description}”?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => del.mutate(t.id, { onError: (e) => Alert.alert('Error', getApiErrorMessage(e)) }),
      },
    ]);
  };

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="px-4 pt-3">
        <MonthSelector month={month} onChange={setMonth} />
      </View>

      {grouped.isLoading ? (
        <LoadingState label="Loading expenses…" />
      ) : grouped.isError ? (
        <ErrorState message={getApiErrorMessage(grouped.error)} onRetry={() => grouped.refetch()} />
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingBottom: 96 }}
          refreshControl={
            <RefreshControl
              refreshing={grouped.isRefetching || budgets.isRefetching}
              onRefresh={() => {
                grouped.refetch();
                budgets.refetch();
              }}
            />
          }
        >
          <Card className="mb-3 flex-row items-center justify-between">
            <Text className="text-sm font-medium text-gray-500 dark:text-slate-400">
              Total this month
            </Text>
            <Text className="text-xl font-bold text-gray-900 dark:text-white">
              {formatMoney(grouped.data?.totalExpense ?? 0, currency)}
            </Text>
          </Card>

          <SectionHeader title="By category" />
          {(grouped.data?.categories.length ?? 0) === 0 ? (
            <Card>
              <EmptyState
                icon="wallet-outline"
                title="No expenses yet"
                subtitle="Tap + to record your first expense for this month."
              />
            </Card>
          ) : (
            grouped.data!.categories.map((g) => (
              <CategoryGroup
                key={g.categoryId}
                group={g}
                currency={currency}
                budget={budgetByCategory.get(g.categoryId)}
                onEditTx={openEdit}
                onDeleteTx={confirmDelete}
              />
            ))
          )}
        </ScrollView>
      )}

      <Fab onPress={openAdd} label="Add expense" />

      <FormModal
        visible={modalOpen}
        title={editing ? 'Edit expense' : 'Add expense'}
        onClose={() => setModalOpen(false)}
      >
        {modalOpen ? <ExpenseForm editing={editing} onClose={() => setModalOpen(false)} /> : null}
      </FormModal>
    </View>
  );
}
