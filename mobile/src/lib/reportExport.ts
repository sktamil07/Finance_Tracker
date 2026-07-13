import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';

import { formatMoney, formatPercent } from '@/lib/money';
import { formatYearMonthLong, formatYearMonthShort } from '@/lib/month';
import type { ReportSummaryResponse } from '@/types/api';

function esc(s: string): string {
  return s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);
}

/**
 * Builds a printable HTML report from the report summary. Rendered to PDF by
 * expo-print. All money/percent values are pre-formatted in JS so the print
 * webview doesn't depend on Intl.
 */
export function buildReportHtml(
  r: ReportSummaryResponse,
  currency: string,
  displayName?: string,
): string {
  const monthlyRows = r.monthly
    .map(
      (m) => `
      <tr>
        <td>${esc(formatYearMonthShort(m.month))}</td>
        <td class="num">${esc(formatMoney(m.salary, currency))}</td>
        <td class="num">${esc(formatMoney(m.invested, currency))}</td>
        <td class="num">${esc(formatMoney(m.expenses, currency))}</td>
        <td class="num">${esc(formatMoney(m.savings, currency))}</td>
        <td class="num">${esc(formatPercent(m.savingsRate))}</td>
      </tr>`,
    )
    .join('');

  const catRows = r.topExpenseCategories.length
    ? r.topExpenseCategories
        .map(
          (c) => `
        <tr>
          <td>${esc(c.categoryName)}</td>
          <td class="num">${esc(formatMoney(c.total, currency))}</td>
          <td class="num">${esc(formatPercent(c.percentageOfWindow))}</td>
        </tr>`,
        )
        .join('')
    : `<tr><td colspan="3" class="empty">No expenses in this window</td></tr>`;

  const t = r.totals;

  return `<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<style>
  * { box-sizing: border-box; }
  body { font-family: -apple-system, "Helvetica Neue", Arial, sans-serif; color: #111827; margin: 0; padding: 32px; }
  .head { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 3px solid #4F46E5; padding-bottom: 16px; margin-bottom: 20px; }
  .brand { font-size: 22px; font-weight: 800; color: #4F46E5; }
  .brand small { display:block; font-size: 12px; font-weight: 600; color:#6B7280; letter-spacing:.08em; text-transform:uppercase; margin-top:2px; }
  .meta { text-align: right; font-size: 12px; color: #6B7280; line-height: 1.6; }
  h2 { font-size: 13px; text-transform: uppercase; letter-spacing: .08em; color: #6B7280; margin: 26px 0 8px; }
  .cards { display: flex; flex-wrap: wrap; gap: 10px; }
  .card { flex: 1 1 30%; border: 1px solid #E5E7EB; border-radius: 10px; padding: 12px 14px; }
  .card .k { font-size: 11px; text-transform: uppercase; letter-spacing:.05em; color: #6B7280; }
  .card .v { font-size: 17px; font-weight: 700; margin-top: 4px; }
  table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
  th, td { padding: 8px 10px; border-bottom: 1px solid #EDF0F5; text-align: left; }
  th { background: #F5F6FA; font-size: 11px; text-transform: uppercase; letter-spacing:.04em; color:#6B7280; }
  td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
  tbody tr:nth-child(even) { background: #FAFBFD; }
  .empty { text-align:center; color:#9CA3AF; padding: 16px; }
  .foot { margin-top: 28px; font-size: 10px; color: #9CA3AF; text-align:center; }
</style>
</head>
<body>
  <div class="head">
    <div>
      <div class="brand">Finance Tracker<small>Summary report</small></div>
    </div>
    <div class="meta">
      ${displayName ? `<div><strong>${esc(displayName)}</strong></div>` : ''}
      <div>${
        r.months === 1
          ? esc(formatYearMonthLong(r.toMonth))
          : `${esc(formatYearMonthLong(r.fromMonth))} &ndash; ${esc(formatYearMonthLong(r.toMonth))}`
      }</div>
      <div>${r.months === 1 ? 'Single month' : `${r.months} month window`}</div>
    </div>
  </div>

  <h2>Totals</h2>
  <div class="cards">
    <div class="card"><div class="k">Salary</div><div class="v">${esc(formatMoney(t.salary, currency))}</div></div>
    <div class="card"><div class="k">Invested</div><div class="v">${esc(formatMoney(t.invested, currency))}</div></div>
    <div class="card"><div class="k">Expenses</div><div class="v">${esc(formatMoney(t.expenses, currency))}</div></div>
    <div class="card"><div class="k">Savings</div><div class="v">${esc(formatMoney(t.savings, currency))}</div></div>
    <div class="card"><div class="k">Avg savings rate</div><div class="v">${esc(formatPercent(t.savingsRate))}</div></div>
  </div>

  <h2>Monthly breakdown</h2>
  <table>
    <thead>
      <tr>
        <th>Month</th><th class="num">Salary</th><th class="num">Invested</th>
        <th class="num">Expenses</th><th class="num">Savings</th><th class="num">Savings %</th>
      </tr>
    </thead>
    <tbody>${monthlyRows || `<tr><td colspan="6" class="empty">No data</td></tr>`}</tbody>
  </table>

  <h2>Top expense categories</h2>
  <table>
    <thead><tr><th>Category</th><th class="num">Total</th><th class="num">% of window</th></tr></thead>
    <tbody>${catRows}</tbody>
  </table>

  <div class="foot">Generated from Finance Tracker · figures in ${esc(currency)}</div>
</body>
</html>`;
}

/**
 * Renders the report to a PDF and opens the system share/save sheet.
 * Returns the file URI. Throws if PDF generation fails.
 */
export async function exportReportPdf(
  r: ReportSummaryResponse,
  currency: string,
  displayName?: string,
): Promise<void> {
  const html = buildReportHtml(r, currency, displayName);
  const { uri } = await Print.printToFileAsync({ html });

  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, {
      mimeType: 'application/pdf',
      dialogTitle: `Finance Tracker report · ${r.fromMonth}–${r.toMonth}`,
      UTI: 'com.adobe.pdf',
    });
  }
}
