// services/stock-movements.service.ts
// Real backend calls for the Stock Movements domain (/api/stock/movements).

import { api } from '@/lib/axios';
import type { Page, StockMovementRequest, StockMovementResponse } from '@/types/models';

export async function getArticleMovements(
  articleId: number,
  page = 0,
  size = 20,
): Promise<Page<StockMovementResponse>> {
  const { data } = await api.get<Page<StockMovementResponse>>(
    `/stock/movements/article/${articleId}`,
    { params: { page, size } },
  );
  return data;
}

export async function getAllMovements(
  page = 0,
  size = 20,
): Promise<Page<StockMovementResponse>> {
  const { data } = await api.get<Page<StockMovementResponse>>(
    '/stock/movements',
    { params: { page, size } },
  );
  return data;
}

export async function createStockMovement(
  payload: StockMovementRequest,
): Promise<StockMovementResponse> {
  const { data } = await api.post<StockMovementResponse>(
    '/stock/movements',
    payload,
  );
  return data;
}
