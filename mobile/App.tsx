import React from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';

import './global.css';
import { queryClient } from '@/api/queryClient';
import { AuthProvider } from '@/auth/AuthContext';
import { ThemeProvider, useTheme } from '@/theme/ThemeProvider';
import { RootNavigator } from '@/navigation/RootNavigator';

/**
 * StatusBar has to live inside ThemeProvider so its bar style follows the
 * resolved light/dark scheme rather than the OS setting alone.
 */
function ThemedStatusBar() {
  const { scheme } = useTheme();
  return <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />;
}

/**
 * Provider order matters:
 *   QueryClient → ThemeProvider → AuthProvider → NavigationContainer
 * AuthProvider consumes the theme (it pushes the server theme preference into
 * ThemeProvider on login), and the navigation container is themed from the
 * resolved palette, so both must sit inside ThemeProvider.
 */
export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <AuthProvider>
              <ThemedStatusBar />
              <RootNavigator />
            </AuthProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
