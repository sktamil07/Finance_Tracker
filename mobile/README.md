# Finance Tracker — Mobile (Expo / React Native)

The iOS + Android client for Finance Tracker. It consumes the same Spring Boot
REST API (`/api/v1`) as the web app and shares its wire contract.

## Stack

- **Expo (managed workflow)** + **TypeScript**, React Native 0.76 / New Architecture
- **React Navigation** — bottom tab bar (Home / Invest / Expense / Tips) inside an
  auth-guarded native stack that also holds Reports, Settings, Categories and
  Budget goals
- **TanStack Query** for all server state; a single **Axios** instance with a
  JWT-attach request interceptor and a single-flight refresh-on-401 response
  interceptor (mirrors the web client)
- **Secure token storage** via `expo-secure-store` (device keychain / keystore) —
  never AsyncStorage
- **React Hook Form + Zod** for every form
- **Charts** with `react-native-svg` (allocation donut, category bars, savings-rate
  trend) — no native chart module, so it runs in Expo Go on both platforms
- **NativeWind** (Tailwind for RN) with light/dark theming driven by the user's
  saved preference
- Money formatted with `Intl.NumberFormat('en-IN', …)` (Hermes ships Intl on SDK
  52; a hand-rolled en-IN grouping fallback guards stripped builds)

## Prerequisites

- Node 18+ and npm
- The backend running and reachable (see the repo root README). Seeded demo login:
  `demo@financetracker.local` / `Demo@12345`
- To run on a device: the **Expo Go** app, or a dev build

## Configure the API base URL

Nothing here is a secret — the client only needs the API origin; tokens are
obtained at runtime and stored in the keychain. Copy the example env file and set
the URL that matches how you run the app:

```bash
cp .env.example .env
```

```
# .env
EXPO_PUBLIC_API_BASE_URL=http://<value-from-table-below>/api/v1
```

| Running on          | Use                                   |
| ------------------- | ------------------------------------- |
| iOS simulator       | `http://localhost:8080/api/v1`        |
| Android emulator    | `http://10.0.2.2:8080/api/v1`         |
| Physical device     | `http://<your-machine-LAN-IP>:8080/api/v1` |
| Staging / prod      | `https://api.your-domain.tld/api/v1`  |

`localhost` on a physical device is the phone itself — use your machine's LAN IP
and make sure the phone and computer are on the same network. The value is also
mirrored into `expo.extra.apiBaseUrl` (see `app.config.ts`) for consumers that
prefer `expo-constants`.

## Install & run

```bash
npm install
npx expo start        # then press "i" (iOS), "a" (Android), or scan the QR in Expo Go
```

Platform shortcuts:

```bash
npm run ios       # expo start --ios
npm run android   # expo start --android
```

If you change `.env`, restart the dev server (Expo inlines `EXPO_PUBLIC_*` at
build time). Start a clean cache with `npx expo start -c` if styles or env look
stale.

## Scripts

| Script              | What it does                                  |
| ------------------- | --------------------------------------------- |
| `npm start`         | Start the Expo dev server                     |
| `npm run android`   | Start and open on Android                     |
| `npm run ios`       | Start and open on iOS                         |
| `npm run typecheck` | `tsc --noEmit` — the project typechecks clean |
| `npm run lint`      | `expo lint`                                   |

## Project layout

```
App.tsx                      Providers: QueryClient → Theme → Auth → Navigation
src/
  api/        Axios client + interceptors, TanStack Query hooks & keys
  auth/       AuthContext (session lifecycle) + secure token storage
  navigation/ Root/auth/app stacks, bottom tabs, themed nav container
  screens/    Dashboard, Invest, Expense, Tips, Reports, Settings,
              Categories, BudgetGoals, and auth/{Login,Register}
  components/ UI kit, forms, charts, state (loading/error/empty), FormModal
  theme/      ThemeProvider + light/dark palette
  lib/        money / month / icon helpers
  types/      api.ts — hand-maintained mirror of the OpenAPI schema
```

## Screens

- **Home / Dashboard** — month selector, metric cards, allocation donut +
  breakdown bars, maturity (bond-due) alerts. Data from `GET /dashboard`.
- **Invest** — add/edit investment form, this-month list with ROI/notes subtitle,
  upcoming SIP & maturity section.
- **Expense** — floating **+** to add, transactions grouped by category with
  totals and % of month, budget alert badges (warning ≥ 80%, over budget > 100%).
- **Tips** — intentional placeholder (the SOP defines no content).
- **Reports** — totals, savings-rate trend, and top expense categories over a
  3 / 6 / 12-month window. Reachable from any tab's header.
- **Settings** — display name, currency, theme, plus links to category management
  and budget goals, and logout. Reachable from any tab's header.

Every data screen has loading / error / empty states; all lists support
pull-to-refresh.

## Regenerating types from the live API (optional)

`src/types/api.ts` is a hand-maintained mirror of the backend's OpenAPI schema so
the app compiles without a running server. To regenerate from the live spec:

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/openapi.d.ts
```

## Notes on the toolchain

Built on **Expo SDK 54** (React Native 0.81, React 19, New Architecture). NativeWind
4.2 + `react-native-css-interop` run their worklet transform through
**Reanimated 4 / `react-native-worklets`**, so `babel.config.js` lists
`react-native-worklets/plugin` (last) rather than the old
`react-native-reanimated/plugin`. Keep those together when bumping the SDK.

Expo Go from the app store only runs the latest SDK — match the project's SDK to
your installed Expo Go, or use a dev build.
