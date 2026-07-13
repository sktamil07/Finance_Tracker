/**
 * Month helpers. The wire format is "YYYY-MM" (java.time.YearMonth). All math is
 * done on that string form so we never accidentally serialise a day component,
 * matching the backend's rule that every month column is pinned to the 1st.
 *
 * We avoid `new Date()` where it matters for correctness and build the "current
 * month" from the local clock only for the initial default selection.
 */

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export function currentYearMonth(): string {
  const now = new Date();
  return toYearMonth(now.getFullYear(), now.getMonth() + 1);
}

export function todayLocalDate(): string {
  const now = new Date();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${m}-${d}`;
}

function toYearMonth(year: number, month1: number): string {
  return `${year}-${String(month1).padStart(2, '0')}`;
}

export function parseYearMonth(ym: string): { year: number; month: number } {
  const [y, m] = ym.split('-').map(Number);
  return { year: y, month: m };
}

export function addMonths(ym: string, delta: number): string {
  const { year, month } = parseYearMonth(ym);
  // month is 1-based; convert to a 0-based absolute index, shift, convert back.
  const abs = year * 12 + (month - 1) + delta;
  const newYear = Math.floor(abs / 12);
  const newMonth = (abs % 12) + 1;
  return toYearMonth(newYear, newMonth);
}

/** "2026-04" -> "April 2026". */
export function formatYearMonthLong(ym: string): string {
  const { year, month } = parseYearMonth(ym);
  return `${MONTH_NAMES[month - 1]} ${year}`;
}

/** "2026-04" -> "Apr 2026". */
export function formatYearMonthShort(ym: string): string {
  const { year, month } = parseYearMonth(ym);
  return `${MONTH_NAMES[month - 1].slice(0, 3)} ${year}`;
}

/** "2026-04" -> "Apr" (for compact axis labels). */
export function formatMonthAbbrev(ym: string): string {
  const { month } = parseYearMonth(ym);
  return MONTH_NAMES[month - 1].slice(0, 3);
}

/** "2026-04-12" -> "12 Apr 2026". */
export function formatLocalDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  return `${d} ${MONTH_NAMES[m - 1].slice(0, 3)} ${y}`;
}

/** True when ym is the current calendar month, so we can disable "next". */
export function isCurrentOrFuture(ym: string): boolean {
  return ym >= currentYearMonth();
}
