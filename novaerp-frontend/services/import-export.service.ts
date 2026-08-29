// services/import-export.service.ts
// CSV export/import for the stock reference lists
// (/api/stock/{articles|categories|suppliers}/export|import).

import { api } from '@/lib/axios';
import type { ImportResultResponse } from '@/types/models';

export type ImportExportEntity = 'articles' | 'categories' | 'suppliers';

export async function exportCsv(entity: ImportExportEntity): Promise<void> {
  const { data } = await api.get<Blob>(`/stock/${entity}/export`, {
    responseType: 'blob',
  });

  const url = window.URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${entity}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
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
