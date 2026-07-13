import React from 'react';
import { Platform } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';

import { HeaderMenu } from '@/components/HeaderMenu';
import { useTheme } from '@/theme/ThemeProvider';
import { DashboardScreen } from '@/screens/DashboardScreen';
import { InvestScreen } from '@/screens/InvestScreen';
import { ExpenseScreen } from '@/screens/ExpenseScreen';
import { TipsScreen } from '@/screens/TipsScreen';
import type { TabParamList } from './types';

const Tab = createBottomTabNavigator<TabParamList>();

type IconName = React.ComponentProps<typeof MaterialCommunityIcons>['name'];

const TAB_ICON: Record<keyof TabParamList, IconName> = {
  Home: 'view-dashboard-outline',
  Invest: 'chart-line',
  Expense: 'wallet-outline',
  Tips: 'lightbulb-on-outline',
};

/**
 * The four SOP tabs: Home (Dashboard), Invest, Expense, Tips. Each tab carries a
 * header with quick links to Reports and Settings via {@link HeaderMenu}.
 */
export function TabNavigator() {
  const { palette } = useTheme();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerStyle: { backgroundColor: palette.card },
        headerTitleStyle: { color: palette.text, fontWeight: '700' },
        headerShadowVisible: false,
        headerRight: () => <HeaderMenu />,
        headerRightContainerStyle: { paddingRight: 12 },
        tabBarActiveTintColor: palette.tabActive,
        tabBarInactiveTintColor: palette.tabInactive,
        tabBarStyle: {
          backgroundColor: palette.tabBar,
          borderTopColor: palette.border,
          height: Platform.OS === 'ios' ? 84 : 62,
          paddingBottom: Platform.OS === 'ios' ? 28 : 8,
          paddingTop: 6,
        },
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        tabBarIcon: ({ color, size }) => (
          <MaterialCommunityIcons name={TAB_ICON[route.name]} size={size} color={color} />
        ),
      })}
    >
      <Tab.Screen name="Home" component={DashboardScreen} options={{ title: 'Dashboard' }} />
      <Tab.Screen name="Invest" component={InvestScreen} options={{ title: 'Invest' }} />
      <Tab.Screen name="Expense" component={ExpenseScreen} options={{ title: 'Expense' }} />
      <Tab.Screen name="Tips" component={TipsScreen} options={{ title: 'Tips' }} />
    </Tab.Navigator>
  );
}
