import React, { useState } from 'react';
import {
  FlatList,
  KeyboardTypeOptions,
  Modal,
  Platform,
  Pressable,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Control, Controller, FieldValues, Path } from 'react-hook-form';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import DateTimePicker, {
  type DateTimePickerEvent,
} from '@react-native-community/datetimepicker';

import { addMonths, formatLocalDate, formatYearMonthLong } from '@/lib/month';
import { useTheme } from '@/theme/ThemeProvider';

function Label({ children }: { children: React.ReactNode }) {
  return (
    <Text className="mb-1.5 text-sm font-medium text-gray-700 dark:text-slate-300">{children}</Text>
  );
}

function ErrorText({ message }: { message?: string }) {
  if (!message) return null;
  return <Text className="mt-1 text-xs text-red-600 dark:text-red-400">{message}</Text>;
}

const inputBase =
  'h-12 rounded-xl border px-3 text-base text-gray-900 dark:text-white bg-white dark:bg-[#1B2436]';

// ---- Text ----------------------------------------------------------------

export function TextField<T extends FieldValues>({
  control,
  name,
  label,
  placeholder,
  keyboardType,
  secureTextEntry,
  autoCapitalize = 'sentences',
  multiline,
  autoFocus,
}: {
  control: Control<T>;
  name: Path<T>;
  label: string;
  placeholder?: string;
  keyboardType?: KeyboardTypeOptions;
  secureTextEntry?: boolean;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  multiline?: boolean;
  autoFocus?: boolean;
}) {
  const { palette } = useTheme();
  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, onBlur, value }, fieldState: { error } }) => (
        <View className="mb-4">
          <Label>{label}</Label>
          <TextInput
            value={value == null ? '' : String(value)}
            onChangeText={onChange}
            onBlur={onBlur}
            placeholder={placeholder}
            placeholderTextColor={palette.textMuted}
            keyboardType={keyboardType}
            secureTextEntry={secureTextEntry}
            autoCapitalize={autoCapitalize}
            autoCorrect={!secureTextEntry}
            multiline={multiline}
            autoFocus={autoFocus}
            className={`${inputBase} ${multiline ? 'h-24 py-3' : ''} ${
              error ? 'border-red-500' : 'border-gray-300 dark:border-slate-600'
            }`}
            style={multiline ? { textAlignVertical: 'top' } : undefined}
          />
          <ErrorText message={error?.message} />
        </View>
      )}
    />
  );
}

// ---- Select (modal list) -------------------------------------------------

export interface SelectOption {
  label: string;
  value: string | number;
  icon?: React.ComponentProps<typeof MaterialCommunityIcons>['name'];
}

export function SelectField<T extends FieldValues>({
  control,
  name,
  label,
  options,
  placeholder = 'Select…',
}: {
  control: Control<T>;
  name: Path<T>;
  label: string;
  options: SelectOption[];
  placeholder?: string;
}) {
  const { palette } = useTheme();
  const [open, setOpen] = useState(false);

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, value }, fieldState: { error } }) => {
        const selected = options.find((o) => o.value === value);
        return (
          <View className="mb-4">
            <Label>{label}</Label>
            <Pressable
              onPress={() => setOpen(true)}
              className={`${inputBase} flex-row items-center justify-between ${
                error ? 'border-red-500' : 'border-gray-300 dark:border-slate-600'
              }`}
            >
              <View className="flex-row items-center">
                {selected?.icon ? (
                  <MaterialCommunityIcons
                    name={selected.icon}
                    size={18}
                    color={palette.text}
                    style={{ marginRight: 8 }}
                  />
                ) : null}
                <Text
                  className={selected ? 'text-gray-900 dark:text-white' : 'text-gray-400 dark:text-slate-500'}
                >
                  {selected ? selected.label : placeholder}
                </Text>
              </View>
              <MaterialCommunityIcons name="chevron-down" size={20} color={palette.textMuted} />
            </Pressable>
            <ErrorText message={error?.message} />

            <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
              <Pressable
                className="flex-1 justify-end bg-black/40"
                onPress={() => setOpen(false)}
              >
                <Pressable
                  className="max-h-[70%] rounded-t-3xl bg-white p-4 dark:bg-[#141C2B]"
                  onPress={(e) => e.stopPropagation()}
                >
                  <Text className="mb-3 text-center text-base font-semibold text-gray-900 dark:text-white">
                    {label}
                  </Text>
                  <FlatList
                    data={options}
                    keyExtractor={(o) => String(o.value)}
                    ItemSeparatorComponent={() => (
                      <View className="h-px bg-gray-100 dark:bg-slate-700/50" />
                    )}
                    renderItem={({ item }) => {
                      const isSel = item.value === value;
                      return (
                        <Pressable
                          onPress={() => {
                            onChange(item.value);
                            setOpen(false);
                          }}
                          className="flex-row items-center justify-between py-3"
                        >
                          <View className="flex-row items-center">
                            {item.icon ? (
                              <MaterialCommunityIcons
                                name={item.icon}
                                size={20}
                                color={palette.text}
                                style={{ marginRight: 10 }}
                              />
                            ) : null}
                            <Text className="text-base text-gray-900 dark:text-white">
                              {item.label}
                            </Text>
                          </View>
                          {isSel ? (
                            <MaterialCommunityIcons name="check" size={20} color={palette.brand} />
                          ) : null}
                        </Pressable>
                      );
                    }}
                  />
                </Pressable>
              </Pressable>
            </Modal>
          </View>
        );
      }}
    />
  );
}

// ---- Switch --------------------------------------------------------------

export function SwitchField<T extends FieldValues>({
  control,
  name,
  label,
  hint,
}: {
  control: Control<T>;
  name: Path<T>;
  label: string;
  hint?: string;
}) {
  const { palette } = useTheme();
  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, value } }) => (
        <View className="mb-4 flex-row items-center justify-between">
          <View className="flex-1 pr-3">
            <Text className="text-sm font-medium text-gray-700 dark:text-slate-300">{label}</Text>
            {hint ? (
              <Text className="mt-0.5 text-xs text-gray-500 dark:text-slate-400">{hint}</Text>
            ) : null}
          </View>
          <Switch
            value={Boolean(value)}
            onValueChange={onChange}
            trackColor={{ false: '#9CA3AF', true: palette.brand }}
            thumbColor="#FFFFFF"
          />
        </View>
      )}
    />
  );
}

// ---- Date (YYYY-MM-DD, no native module) ---------------------------------

/** "YYYY-MM-DD" -> local Date (or null). */
function ymdToDate(ymd: string | null | undefined): Date | null {
  if (!ymd) return null;
  const [y, m, d] = ymd.split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

/** local Date -> "YYYY-MM-DD" (no timezone shift). */
function dateToYmd(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

/**
 * Date field backed by the platform's native calendar picker
 * (@react-native-community/datetimepicker, bundled in Expo Go). Tapping the
 * field opens the OS date dialog; the value is stored as the wire "YYYY-MM-DD".
 * When `optional`, a clear (×) affordance resets it to empty.
 */
export function DateField<T extends FieldValues>({
  control,
  name,
  label,
  optional = false,
}: {
  control: Control<T>;
  name: Path<T>;
  label: string;
  optional?: boolean;
}) {
  const { palette } = useTheme();
  const [show, setShow] = useState(false);

  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, value }, fieldState: { error } }) => {
        const selected = ymdToDate(value);

        const handleChange = (event: DateTimePickerEvent, picked?: Date) => {
          if (Platform.OS === 'android') {
            setShow(false);
            if (event.type === 'set' && picked) onChange(dateToYmd(picked));
          } else if (picked) {
            // iOS spinner updates live; the modal's Done button dismisses.
            onChange(dateToYmd(picked));
          }
        };

        return (
          <View className="mb-4">
            <Label>{label}</Label>
            <View className="flex-row items-center">
              <Pressable
                onPress={() => setShow(true)}
                className={`${inputBase} flex-1 flex-row items-center justify-between ${
                  error ? 'border-red-500' : 'border-gray-300 dark:border-slate-600'
                }`}
              >
                <Text
                  className={selected ? 'text-gray-900 dark:text-white' : 'text-gray-400 dark:text-slate-500'}
                >
                  {selected ? formatLocalDate(value) : 'Select a date'}
                </Text>
                <MaterialCommunityIcons name="calendar-month-outline" size={20} color={palette.textMuted} />
              </Pressable>
              {optional && selected ? (
                <Pressable
                  onPress={() => onChange('')}
                  accessibilityLabel="Clear date"
                  className="ml-2 h-12 w-11 items-center justify-center rounded-xl bg-gray-200 dark:bg-slate-700"
                >
                  <MaterialCommunityIcons name="close" size={18} color={palette.text} />
                </Pressable>
              ) : null}
            </View>
            <ErrorText message={error?.message} />

            {/* Android renders a dialog; iOS renders inline, so we host it in a sheet. */}
            {show && Platform.OS === 'android' ? (
              <DateTimePicker
                value={selected ?? new Date()}
                mode="date"
                display="calendar"
                onChange={handleChange}
              />
            ) : null}

            {Platform.OS === 'ios' ? (
              <Modal visible={show} transparent animationType="fade" onRequestClose={() => setShow(false)}>
                <Pressable className="flex-1 justify-end bg-black/40" onPress={() => setShow(false)}>
                  <Pressable className="rounded-t-3xl bg-white p-4 dark:bg-[#141C2B]" onPress={(e) => e.stopPropagation()}>
                    <DateTimePicker
                      value={selected ?? new Date()}
                      mode="date"
                      display="inline"
                      onChange={handleChange}
                    />
                    <Pressable
                      onPress={() => setShow(false)}
                      className="mt-2 h-12 items-center justify-center rounded-xl bg-brand-600"
                    >
                      <Text className="text-base font-semibold text-white">Done</Text>
                    </Pressable>
                  </Pressable>
                </Pressable>
              </Modal>
            ) : null}
          </View>
        );
      }}
    />
  );
}

// ---- Month stepper -------------------------------------------------------

export function MonthField<T extends FieldValues>({
  control,
  name,
  label,
}: {
  control: Control<T>;
  name: Path<T>;
  label: string;
}) {
  const { palette } = useTheme();
  return (
    <Controller
      control={control}
      name={name}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <View className="mb-4">
          <Label>{label}</Label>
          <View className="h-12 flex-row items-center justify-between rounded-xl border border-gray-300 bg-white px-1 dark:border-slate-600 dark:bg-[#1B2436]">
            <Pressable
              onPress={() => onChange(addMonths(value, -1))}
              className="h-10 w-10 items-center justify-center"
            >
              <MaterialCommunityIcons name="chevron-left" size={24} color={palette.text} />
            </Pressable>
            <Text className="text-base font-medium text-gray-900 dark:text-white">
              {value ? formatYearMonthLong(value) : '—'}
            </Text>
            <Pressable
              onPress={() => onChange(addMonths(value, 1))}
              className="h-10 w-10 items-center justify-center"
            >
              <MaterialCommunityIcons name="chevron-right" size={24} color={palette.text} />
            </Pressable>
          </View>
          <ErrorText message={error?.message} />
        </View>
      )}
    />
  );
}
