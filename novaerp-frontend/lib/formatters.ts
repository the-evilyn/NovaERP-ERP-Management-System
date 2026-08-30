// lib/formatters.ts
// Centralized formatting utilities for NovaERP (Moroccan Industrial ERP)

const currencyFormatter = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const currencyCompactFormatter = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  notation: "compact",
  maximumFractionDigits: 1,
});

const dateFormatter = new Intl.DateTimeFormat("fr-MA", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

const dateTimeFormatter = new Intl.DateTimeFormat("fr-MA", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

const numberFormatter = new Intl.NumberFormat("fr-MA", {
  maximumFractionDigits: 2,
});

/**
 * Formats a monetary value in Moroccan Dirham (MAD).
 * Example: 12500.5 -> "12 500,50 MAD"
 */
export function formatCurrency(
  value: number | string | null | undefined,
  options?: { compact?: boolean }
): string {
  if (value === null || value === undefined) return "0,00 MAD";
  const num = typeof value === "string" ? Number.parseFloat(value) : value;
  if (Number.isNaN(num)) return "0,00 MAD";
  return options?.compact
    ? currencyCompactFormatter.format(num)
    : currencyFormatter.format(num);
}

/**
 * Formats a date into standard French Moroccan date (DD/MM/YYYY).
 */
export function formatDate(
  value: Date | string | number | null | undefined
): string {
  if (!value) return "-";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return dateFormatter.format(date);
}

/**
 * Formats a date-time into readable French Moroccan format (e.g. 15 oct. 2026, 14:30).
 */
export function formatDateTime(
  value: Date | string | number | null | undefined
): string {
  if (!value) return "-";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return dateTimeFormatter.format(date);
}

/**
 * Formats a numerical quantity with optional unit of measure.
 * Example: formatQuantity(1500, "kg") -> "1 500 kg"
 */
export function formatQuantity(
  value: number | string | null | undefined,
  unit?: string | null
): string {
  if (value === null || value === undefined) return unit ? `0 ${unit}` : "0";
  const num = typeof value === "string" ? Number.parseFloat(value) : value;
  if (Number.isNaN(num)) return unit ? `0 ${unit}` : "0";
  const formatted = numberFormatter.format(num);
  return unit ? `${formatted} ${unit}` : formatted;
}

/**
 * Formats a percentage.
 * Example: formatPercentage(45.2) -> "45,2 %"
 */
export function formatPercentage(
  value: number | string | null | undefined,
  decimals: number = 1
): string {
  if (value === null || value === undefined) return "0 %";
  const num = typeof value === "string" ? Number.parseFloat(value) : value;
  if (Number.isNaN(num)) return "0 %";
  return `${num.toLocaleString("fr-MA", { maximumFractionDigits: decimals })} %`;
}

/**
 * Formats a generic number with Moroccan thousands/decimal separators.
 */
export function formatNumber(
  value: number | string | null | undefined,
  decimals: number = 2
): string {
  if (value === null || value === undefined) return "0";
  const num = typeof value === "string" ? Number.parseFloat(value) : value;
  if (Number.isNaN(num)) return "0";
  return num.toLocaleString("fr-MA", { maximumFractionDigits: decimals });
}
