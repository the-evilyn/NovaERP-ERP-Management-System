// services/import-export.service.ts
// CSV export/import for the stock reference lists
// (/api/stock/{articles|categories|suppliers}/export|import).

import { api } from '@/lib/axios';
import { downloadCsvBlob } from '@/lib/csv';
import type { ImportResultResponse } from '@/types/models';

export type ImportExportEntity =
  | 'articles'
  | 'categories'
  | 'suppliers'
  | 'clients'
  | 'stock-movements'
  | 'sale-orders';

export async function exportCsv(
  entity: ImportExportEntity,
  params?: Record<string, string | number | boolean | undefined>,
): Promise<void> {
  let endpoint = `/stock/${entity}/export`;
  if (entity === 'clients') {
    endpoint = '/clients/export';
  } else if (entity === 'stock-movements') {
    endpoint = '/stock/movements/export';
  } else if (entity === 'sale-orders') {
    endpoint = '/sales/orders/export';
  }

  const { data } = await api.get<Blob>(endpoint, {
    params,
    responseType: 'blob',
  });

  downloadCsvBlob(data, `${entity}.csv`);
}

export async function importCsv(
  entity: ImportExportEntity,
  file: File,
): Promise<ImportResultResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const { data } = await api.post<ImportResultResponse>(
    `/stock/${entity}/import`,
    formData,
  );
  return data;
}
