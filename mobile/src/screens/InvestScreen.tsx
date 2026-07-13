import React, { useMemo, useState } from 'react';
import { Alert, Pressable, RefreshControl, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import {
  useCategories,
  useCreateInvestment,
  useDeleteInvestment,
  useInvestments,
  useUpcomingInvestments,
  useUpdateInvestment,
} from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { useCurrency } from '@/auth/AuthContext';
import { CategoryIcon } from '@/components/CategoryIcon';
import { Fab, FormModal } from '@/components/FormModal';
import {
  DateField,
  MonthField,
  SelectField,
  SwitchField,
  TextField,
  type SelectOption,
} from '@/components/form';
import { MonthSelector } from '@/components/MonthSelector';
import { Button, Badge, Card, SectionHeader } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { resolveCategoryIcon } from '@/lib/icons';
import { formatMoney } from '@/lib/money';
import { currentYearMonth, formatLocalDate } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';
import type { InvestmentRequest, InvestmentResponse } from '@/types/api';

const schema = z
  .object({
    name: z.string().min(1, 'Name is required').max(120, 'Name is too long'),
    amount: z
      .string()
      .min(1, 'Amount is required')
      .refine((v) => Number(v) > 0, 'Enter an amount greater than 0'),
    categoryId: z.number().refine((v) => v > 0, 'Select a category'),
    month: z.string().min(1),
    roiNotes: z.string().optional(),
    recurring: z.boolean(),
    recurringDayOfMonth: z.string().optional(),
    maturityDate: z.string().optional(),
  })
  .superRefine((val, ctx) => {
    if (val.recurring) {
      const d = Number(val.recurringDayOfMonth);
      if (!val.recurringDayOfMonth || !Number.isInteger(d) || d < 1 || d > 28) {
        ctx.addIssue({ code: 'custom', path: ['recurringDayOfMonth'], message: 'Enter a day 1–28' });
      }
    }
    if (val.maturityDate && !/^\d{4}-\d{2}-\d{2}$/.test(val.maturityDate)) {
      ctx.addIssue({ code: 'custom', path: ['maturityDate'], message: 'Use YYYY-MM-DD' });
    }
  });

type FormValues = z.infer<typeof schema>;

function toDefaults(month: string, inv?: InvestmentResponse): FormValues {
  return {
    name: inv?.name ?? '',
    amount: inv ? String(inv.amount) : '',
    categoryId: inv?.categoryId ?? 0,
    month: inv?.month ?? month,
    roiNotes: inv?.roiNotes ?? '',
    recurring: inv?.recurring ?? false,
    recurringDayOfMonth: inv?.recurringDayOfMonth ? String(inv.recurringDayOfMonth) : '',
    maturityDate: inv?.maturityDate ?? '',
  };
}

function InvestmentForm({
  month,
  editing,
  onClose,
}: {
  month: string;
  editing?: InvestmentResponse;
  onClose: () => void;
}) {
  const categories = useCategories('INVESTMENT');
  const create = useCreateInvestment();
  const update = useUpdateInvestment();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, watch, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toDefaults(month, editing),
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
    const payload: InvestmentRequest = {
      name: v.name.trim(),
      amount: Number(v.amount),
      categoryId: v.categoryId,
      month: v.month,
      roiNotes: v.roiNotes?.trim() ? v.roiNotes.trim() : null,
      recurring: v.recurring,
      recurringDayOfMonth: v.recurring ? Number(v.recurringDayOfMonth) : null,
      maturityDate: v.maturityDate?.trim() ? v.maturityDate.trim() : null,
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
      <TextField control={control} name="name" label="Name" placeholder="e.g. Index SIP" />
      <TextField control={control} name="amount" label="Amount" placeholder="0" keyboardType="decimal-pad" />
      <SelectField
        control={control}
        name="categoryId"
        label="Category"
        options={options}
        placeholder={categories.isLoading ? 'Loading…' : 'Select a category'}
      />
      <MonthField control={control} name="month" label="Month" />
      <TextField control={control} name="roiNotes" label="ROI / Notes (optional)" placeholder="e.g. 12% p.a." multiline />
      <SwitchField control={control} name="recurring" label="Recurring (SIP)" hint="Repeats on a day each month" />
      {recurring ? (
        <TextField
          control={control}
          name="recurringDayOfMonth"
          label="Day of month (1–28)"
          placeholder="e.g. 5"
          keyboardType="number-pad"
        />
      ) : null}
      <DateField control={control} name="maturityDate" label="Maturity date (optional)" optional />

      {submitError ? (
        <Text className="mb-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <Button
        label={editing ? 'Save changes' : 'Add investment'}
        onPress={onSubmit}
        loading={formState.isSubmitting}
        fullWidth
      />
    </View>
  );
}

function InvestmentRow({
  inv,
  currency,
  onEdit,
  onDelete,
}: {
  inv: InvestmentResponse;
  currency: string;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { palette } = useTheme();
  return (
    <Card className="mb-2">
      <View className="flex-row items-center">
        <CategoryIcon slug={inv.categoryIcon} color={palette.brand} />
        <Pressable className="ml-3 flex-1" onPress={onEdit}>
          <View className="flex-row items-center justify-between">
            <Text className="flex-1 pr-2 font-semibold text-gray-900 dark:text-white" numberOfLines={1}>
              {inv.name}
            </Text>
            <Text className="font-bold text-gray-900 dark:text-white">
              {formatMoney(inv.amount, currency)}
            </Text>
          </View>
          <Text className="text-sm text-gray-500 dark:text-slate-400" numberOfLines={1}>
            {inv.categoryName}
            {inv.roiNotes ? ` · ${inv.roiNotes}` : ''}
          </Text>
          <View className="mt-1 flex-row gap-2">
            {inv.recurring ? <Badge label={`SIP · day ${inv.recurringDayOfMonth}`} tone="brand" /> : null}
            {inv.maturityDate ? (
              <Badge label={`Matures ${formatLocalDate(inv.maturityDate)}`} tone="warning" />
            ) : null}
          </View>
        </Pressable>
        <Pressable
          onPress={onDelete}
          hitSlop={8}
          accessibilityLabel="Delete investment"
          className="ml-2 h-9 w-9 items-center justify-center rounded-full active:bg-red-50 dark:active:bg-red-900/30"
        >
          <MaterialCommunityIcons name="trash-can-outline" size={20} color={palette.danger} />
        </Pressable>
      </View>
    </Card>
  );
}

export function InvestScreen() {
  const [month, setMonth] = useState(currentYearMonth());
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<InvestmentResponse | undefined>(undefined);
  const currency = useCurrency();
  const { palette } = useTheme();

  const investments = useInvestments(month);
  const upcoming = useUpcomingInvestments();
  const del = useDeleteInvestment();

  const openAdd = () => {
    setEditing(undefined);
    setModalOpen(true);
  };
  const openEdit = (inv: InvestmentResponse) => {
    setEditing(inv);
    setModalOpen(true);
  };

  const confirmDelete = (inv: InvestmentResponse) => {
    Alert.alert('Delete investment', `Delete “${inv.name}”?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => del.mutate(inv.id, { onError: (e) => Alert.alert('Error', getApiErrorMessage(e)) }),
      },
    ]);
  };

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="px-4 pt-3">
        <MonthSelector month={month} onChange={setMonth} />
      </View>

      {investments.isLoading ? (
        <LoadingState label="Loading investments…" />
      ) : investments.isError ? (
        <ErrorState message={getApiErrorMessage(investments.error)} onRetry={() => investments.refetch()} />
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingBottom: 96 }}
          refreshControl={
            <RefreshControl
              refreshing={investments.isRefetching || upcoming.isRefetching}
              onRefresh={() => {
                investments.refetch();
                upcoming.refetch();
              }}
            />
          }
        >
          {/* Upcoming SIP / maturities */}
          <SectionHeader title="Upcoming (SIP & maturities)" />
          {(upcoming.data?.length ?? 0) === 0 ? (
            <Card>
              <EmptyState icon="calendar-clock" title="Nothing upcoming" />
            </Card>
          ) : (
            upcoming.data!.map((u) => (
              <Card key={`${u.kind}-${u.investmentId}`} className="mb-2">
                <View className="flex-row items-center">
                  <MaterialCommunityIcons
                    name={u.kind === 'SIP' ? 'autorenew' : 'flag-checkered'}
                    size={20}
                    color={u.kind === 'SIP' ? palette.brand : palette.warning}
                  />
                  <View className="ml-3 flex-1">
                    <Text className="font-semibold text-gray-900 dark:text-white" numberOfLines={1}>
                      {u.name}
                    </Text>
                    <Text className="text-sm text-gray-500 dark:text-slate-400">
                      {u.kind === 'SIP' ? 'SIP due' : 'Matures'} · {formatLocalDate(u.dueDate)}
                    </Text>
                  </View>
                  <Text className="font-bold text-gray-900 dark:text-white">
                    {formatMoney(u.amount, currency)}
                  </Text>
                </View>
              </Card>
            ))
          )}

          {/* This month's investments */}
          <SectionHeader title="This month" />
          {(investments.data?.length ?? 0) === 0 ? (
            <Card>
              <EmptyState
                icon="chart-line"
                title="No investments yet"
                subtitle="Tap + to add your first investment for this month."
              />
            </Card>
          ) : (
            investments.data!.map((inv) => (
              <InvestmentRow
                key={inv.id}
                inv={inv}
                currency={currency}
                onEdit={() => openEdit(inv)}
                onDelete={() => confirmDelete(inv)}
              />
            ))
          )}
        </ScrollView>
      )}

      <Fab onPress={openAdd} label="Add investment" />

      <FormModal
        visible={modalOpen}
        title={editing ? 'Edit investment' : 'Add investment'}
        onClose={() => setModalOpen(false)}
      >
        {modalOpen ? (
          <InvestmentForm month={month} editing={editing} onClose={() => setModalOpen(false)} />
        ) : null}
      </FormModal>
    </View>
  );
}
