/**
 * Money + percentage formatting.
 *
 * The brief mandates Intl.NumberFormat('en-IN', …). Hermes has shipped Intl
 * (including the Indian grouping, 1,00,000) since Expo SDK 50 / RN 0.74 when the
 * app is built normally — no extra polyfill is required for SDK 52. We still
 * guard the call: if a stripped Hermes build lacks Intl, we fall back to a
 * hand-rolled en-IN grouping so amounts never render as a raw float.
 */

const hasIntl =
  typeof Intl !== 'undefined' &&
  typeof Intl.NumberFormat !== 'undefined' &&
  (() => {
    try {
      // Probe that the ICU data actually knows en-IN grouping.
      return new Intl.NumberFormat('en-IN').format(100000).includes(',');
    } catch {
      return false;
    }
  })();

// Cache formatters by currency — constructing them is comparatively expensive.
const currencyFormatters = new Map<string, Intl.NumberFormat>();

function currencyFormatter(currencyCode: string): Intl.NumberFormat {
  let fmt = currencyFormatters.get(currencyCode);
  if (!fmt) {
    fmt = new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currencyCode,
      maximumFractionDigits: 2,
    });
    currencyFormatters.set(currencyCode, fmt);
  }
  return fmt;
}

/** Manual en-IN grouping fallback: 1234567.5 -> "12,34,567.50". */
function groupEnIn(value: number): string {
  const neg = value < 0;
  const [intPart, fracPart] = Math.abs(value).toFixed(2).split('.');
  let out = intPart;
  if (intPart.length > 3) {
    const last3 = intPart.slice(-3);
    const rest = intPart.slice(0, -3);
    out = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last3;
  }
  return `${neg ? '-' : ''}${out}.${fracPart}`;
}

const CURRENCY_SYMBOLS: Record<string, string> = {
  INR: '₹',
  USD: '$',
  EUR: '€',
  GBP: '£',
  JPY: '¥',
  AUD: 'A$',
  CAD: 'C$',
  SGD: 'S$',
  AED: 'د.إ',
};

/** Format an amount as currency, e.g. formatMoney(191000, 'INR') -> "₹1,91,000.00". */
export function formatMoney(value: number | null | undefined, currencyCode = 'INR'): string {
  const n = typeof value === 'number' && Number.isFinite(value) ? value : 0;
  if (hasIntl) {
    try {
      return currencyFormatter(currencyCode).format(n);
    } catch {
      // Unknown currency code — fall through to symbol + grouping.
    }
  }
  const symbol = CURRENCY_SYMBOLS[currencyCode] ?? `${currencyCode} `;
  return `${symbol}${groupEnIn(n)}`;
}

/** Compact form for cards: 191000 -> "₹1.91L", 130000 -> "₹1.30L". */
export function formatMoneyCompact(value: number | null | undefined, currencyCode = 'INR'): string {
  const n = typeof value === 'number' && Number.isFinite(value) ? value : 0;
  const symbol = CURRENCY_SYMBOLS[currencyCode] ?? '';
  const abs = Math.abs(n);
  const sign = n < 0 ? '-' : '';
  if (abs >= 1_00_00_000) return `${sign}${symbol}${(abs / 1_00_00_000).toFixed(2)}Cr`;
  if (abs >= 1_00_000) return `${sign}${symbol}${(abs / 1_00_000).toFixed(2)}L`;
  if (abs >= 1_000) return `${sign}${symbol}${(abs / 1_000).toFixed(1)}K`;
  return formatMoney(n, currencyCode);
}

/** Percentage with a fixed 2dp and sign, e.g. 10.42 -> "10.42%". */
export function formatPercent(value: number | null | undefined, dp = 2): string {
  const n = typeof value === 'number' && Number.isFinite(value) ? value : 0;
  return `${n.toFixed(dp)}%`;
}
