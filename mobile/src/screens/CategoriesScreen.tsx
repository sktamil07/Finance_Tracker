import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import {
  useCategories,
  useCreateCategory,
  useDeleteCategory,
  useUpdateCategory,
} from '@/api/hooks';
import { getApiErrorMessage } from '@/api/errors';
import { CategoryIcon } from '@/components/CategoryIcon';
import { Fab, FormModal } from '@/components/FormModal';
import { TextField } from '@/components/form';
import { Badge, Button, Card, Segmented } from '@/components/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/states';
import { ICON_CHOICES, resolveCategoryIcon } from '@/lib/icons';
import { useTheme } from '@/theme/ThemeProvider';
import type { CategoryResponse, CategoryType } from '@/types/api';

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(60, 'Too long'),
});
type FormValues = z.infer<typeof schema>;

function IconPicker({ value, onChange }: { value: string; onChange: (slug: string) => void }) {
  const { palette } = useTheme();
  return (
    <View className="mb-4">
      <Text className="mb-1.5 text-sm font-medium text-gray-700 dark:text-slate-300">Icon</Text>
      <View className="flex-row flex-wrap gap-2">
        {ICON_CHOICES.map((slug) => {
          const active = slug === value;
          return (
            <Pressable
              key={slug}
              onPress={() => onChange(slug)}
              className={`h-11 w-11 items-center justify-center rounded-xl border ${
                active
                  ? 'border-brand-600 bg-brand-100 dark:bg-brand-900/40'
                  : 'border-gray-200 bg-white dark:border-slate-600 dark:bg-[#1B2436]'
              }`}
            >
              <MaterialCommunityIcons
                name={resolveCategoryIcon(slug)}
                size={22}
                color={active ? palette.brand : palette.textMuted}
              />
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

function CategoryForm({
  type,
  editing,
  onClose,
}: {
  type: CategoryType;
  editing?: CategoryResponse;
  onClose: () => void;
}) {
  const create = useCreateCategory();
  const update = useUpdateCategory();
  const [icon, setIcon] = useState<string>(editing?.icon ?? ICON_CHOICES[0]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { control, handleSubmit, formState } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: editing?.name ?? '' },
  });

  const onSubmit = handleSubmit(async (v) => {
    setSubmitError(null);
    try {
      if (editing) {
        await update.mutateAsync({ id: editing.id, body: { name: v.name.trim(), icon } });
      } else {
        await create.mutateAsync({ name: v.name.trim(), type, icon });
      }
      onClose();
    } catch (err) {
      setSubmitError(getApiErrorMessage(err));
    }
  });

  return (
    <View>
      <TextField control={control} name="name" label="Name" placeholder="e.g. Dining out" />
      <IconPicker value={icon} onChange={setIcon} />
      {submitError ? (
        <Text className="mb-3 text-sm text-red-600 dark:text-red-400">{submitError}</Text>
      ) : null}
      <Button
        label={editing ? 'Save changes' : `Add ${type === 'EXPENSE' ? 'expense' : 'investment'} category`}
        onPress={onSubmit}
        loading={formState.isSubmitting}
        fullWidth
      />
    </View>
  );
}

export function CategoriesScreen() {
  const [type, setType] = useState<CategoryType>('EXPENSE');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CategoryResponse | undefined>(undefined);
  const { palette } = useTheme();

  const categories = useCategories(type);
  const del = useDeleteCategory();

  const openAdd = () => {
    setEditing(undefined);
    setModalOpen(true);
  };
  const openEdit = (c: CategoryResponse) => {
    if (c.systemDefault) return; // system categories are read-only
    setEditing(c);
    setModalOpen(true);
  };
  const confirmDelete = (c: CategoryResponse) => {
    Alert.alert('Delete category', `Delete “${c.name}”? Existing records keep their history.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => del.mutate(c.id, { onError: (e) => Alert.alert('Error', getApiErrorMessage(e)) }),
      },
    ]);
  };

  return (
    <View className="flex-1 bg-gray-100 dark:bg-[#0B1220]">
      <View className="px-4 pt-3">
        <Segmented
          value={type}
          onChange={setType}
          options={[
            { label: 'Expense', value: 'EXPENSE' },
            { label: 'Investment', value: 'INVESTMENT' },
          ]}
        />
      </View>

      {categories.isLoading ? (
        <LoadingState label="Loading categories…" />
      ) : categories.isError ? (
        <ErrorState message={getApiErrorMessage(categories.error)} onRetry={() => categories.refetch()} />
      ) : (
        <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 96 }}>
          {(categories.data?.length ?? 0) === 0 ? (
            <Card>
              <EmptyState icon="shape-outline" title="No categories" subtitle="Tap + to add one." />
            </Card>
          ) : (
            categories.data!.map((c) => (
              <Card key={c.id} className="mb-2">
                <View className="flex-row items-center">
                  <CategoryIcon slug={c.icon} color={palette.categorical[0]} />
                  <Pressable className="ml-3 flex-1" onPress={() => openEdit(c)}>
                    <View className="flex-row items-center">
                      <Text className="font-semibold text-gray-900 dark:text-white">{c.name}</Text>
                      {c.systemDefault ? (
                        <View className="ml-2">
                          <Badge label="System" tone="neutral" />
                        </View>
                      ) : null}
                    </View>
                  </Pressable>
                  {c.systemDefault ? null : (
                    <Pressable
                      onPress={() => confirmDelete(c)}
                      hitSlop={8}
                      accessibilityLabel="Delete category"
                      className="h-9 w-9 items-center justify-center rounded-full active:bg-red-50 dark:active:bg-red-900/30"
                    >
                      <MaterialCommunityIcons name="trash-can-outline" size={20} color={palette.danger} />
                    </Pressable>
                  )}
                </View>
              </Card>
            ))
          )}
        </ScrollView>
      )}

      <Fab onPress={openAdd} label="Add category" />

      <FormModal
        visible={modalOpen}
        title={editing ? 'Edit category' : 'New category'}
        onClose={() => setModalOpen(false)}
      >
        {modalOpen ? (
          <CategoryForm type={type} editing={editing} onClose={() => setModalOpen(false)} />
        ) : null}
      </FormModal>
    </View>
  );
}
