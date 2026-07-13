import React from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { useTheme } from '@/theme/ThemeProvider';

/**
 * A bottom-sheet style modal that hosts a form. Keyboard-aware and scrollable so
 * long forms stay usable on small screens. Tapping the scrim or the close button
 * dismisses.
 */
export function FormModal({
  visible,
  title,
  onClose,
  children,
}: {
  visible: boolean;
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  const { palette } = useTheme();
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        className="flex-1"
      >
        <Pressable className="flex-1 justify-end bg-black/40" onPress={onClose}>
          <Pressable
            className="max-h-[88%] rounded-t-3xl bg-white dark:bg-[#141C2B]"
            onPress={(e) => e.stopPropagation()}
          >
            <View className="flex-row items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-slate-700/60">
              <Text className="text-lg font-bold text-gray-900 dark:text-white">{title}</Text>
              <Pressable
                onPress={onClose}
                hitSlop={8}
                accessibilityRole="button"
                accessibilityLabel="Close"
                className="h-8 w-8 items-center justify-center rounded-full active:bg-gray-100 dark:active:bg-slate-700"
              >
                <MaterialCommunityIcons name="close" size={20} color={palette.textMuted} />
              </Pressable>
            </View>
            <ScrollView
              className="px-5"
              contentContainerStyle={{ paddingVertical: 16 }}
              keyboardShouldPersistTaps="handled"
            >
              {children}
            </ScrollView>
          </Pressable>
        </Pressable>
      </KeyboardAvoidingView>
    </Modal>
  );
}

/** Floating action button, bottom-right. */
export function Fab({ onPress, label = 'Add' }: { onPress: () => void; label?: string }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      className="absolute bottom-6 right-5 h-14 w-14 items-center justify-center rounded-full bg-brand-600 shadow-lg active:bg-brand-700"
      style={{
        shadowColor: '#000',
        shadowOpacity: 0.25,
        shadowRadius: 8,
        shadowOffset: { width: 0, height: 4 },
        elevation: 6,
      }}
    >
      <MaterialCommunityIcons name="plus" size={28} color="#FFFFFF" />
    </Pressable>
  );
}
