import React from 'react';
import { View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ActivityIndicator } from 'react-native';

import { useAuth } from '@/auth/AuthContext';
import { useTheme } from '@/theme/ThemeProvider';
import { LoginScreen } from '@/screens/auth/LoginScreen';
import { RegisterScreen } from '@/screens/auth/RegisterScreen';
import { IncomeScreen } from '@/screens/IncomeScreen';
import { ReportsScreen } from '@/screens/ReportsScreen';
import { SettingsScreen } from '@/screens/SettingsScreen';
import { CategoriesScreen } from '@/screens/CategoriesScreen';
import { BudgetGoalsScreen } from '@/screens/BudgetGoalsScreen';
import { TabNavigator } from './TabNavigator';
import { navThemeFor } from './navTheme';
import type { AppStackParamList, AuthStackParamList } from './types';

const AuthStack = createNativeStackNavigator<AuthStackParamList>();
const AppStack = createNativeStackNavigator<AppStackParamList>();

function AuthNavigator() {
  return (
    <AuthStack.Navigator screenOptions={{ headerShown: false }}>
      <AuthStack.Screen name="Login" component={LoginScreen} />
      <AuthStack.Screen name="Register" component={RegisterScreen} />
    </AuthStack.Navigator>
  );
}

function AppNavigator() {
  const { palette } = useTheme();
  const stackHeader = {
    headerStyle: { backgroundColor: palette.card },
    headerTitleStyle: { color: palette.text },
    headerTintColor: palette.brand,
    headerShadowVisible: false,
    contentStyle: { backgroundColor: palette.background },
  } as const;

  return (
    <AppStack.Navigator screenOptions={stackHeader}>
      <AppStack.Screen name="Tabs" component={TabNavigator} options={{ headerShown: false }} />
      <AppStack.Screen name="Income" component={IncomeScreen} options={{ title: 'Income' }} />
      <AppStack.Screen name="Reports" component={ReportsScreen} options={{ title: 'Reports' }} />
      <AppStack.Screen name="Settings" component={SettingsScreen} options={{ title: 'Settings' }} />
      <AppStack.Screen
        name="Categories"
        component={CategoriesScreen}
        options={{ title: 'Manage categories' }}
      />
      <AppStack.Screen
        name="BudgetGoals"
        component={BudgetGoalsScreen}
        options={{ title: 'Budget goals' }}
      />
    </AppStack.Navigator>
  );
}

/**
 * Auth-guarded root. While the keychain is being read we show a spinner; then
 * we mount either the auth stack or the app stack. Switching `status` swaps the
 * whole tree, so a failed refresh (auto-logout) drops the user back to Login.
 */
export function RootNavigator() {
  const { status } = useAuth();
  const { palette } = useTheme();

  return (
    <NavigationContainer theme={navThemeFor(palette)}>
      {status === 'loading' ? (
        <View
          style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: palette.background }}
        >
          <ActivityIndicator size="large" color={palette.brand} />
        </View>
      ) : status === 'authenticated' ? (
        <AppNavigator />
      ) : (
        <AuthNavigator />
      )}
    </NavigationContainer>
  );
}
