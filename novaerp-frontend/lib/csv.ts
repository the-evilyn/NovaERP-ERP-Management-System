// lib/csv.ts
// Minimal CSV parsing for simple, comma-delimited files without quoted/escaped fields.

export function parseCsv(text: string): Record<string, string>[] {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.length < 2) return [];

  const headers = lines[0].split(",").map((h) => h.trim());
  return lines.slice(1).map((line) => {
    const values = line.split(",").map((v) => v.trim());
    return Object.fromEntries(
      headers.map((header, i) => [header, values[i] ?? ""]),
    );
  });
}
