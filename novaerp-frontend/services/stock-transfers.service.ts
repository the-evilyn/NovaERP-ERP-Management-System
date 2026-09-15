// services/stock-transfers.service.ts
// API service for Stock Transfers (/api/stock/transfers)

import { api } from '@/lib/axios';
import type {
  Page,
  StockTransferRequest,
  StockTransferResponse,
  StockTransferStatus,
} from '@/types/models';

export async function getStockTransfers(
  page = 0,
  size = 10,
  status?: StockTransferStatus,
  warehouseId?: number,
): Promise<Page<StockTransferResponse>> {
  const { data } = await api.get<Page<StockTransferResponse>>('/stock/transfers', {
    params: {
      page,
      size,
      status: status || undefined,
      warehouseId: warehouseId || undefined,
    },
  });
  return data;
}

export async function getStockTransfer(id: number): Promise<StockTransferResponse> {
  const { data } = await api.get<StockTransferResponse>(`/stock/transfers/${id}`);
  return data;
}

export async function createStockTransfer(
  payload: StockTransferRequest,
): Promise<StockTransferResponse> {
  const { data } = await api.post<StockTransferResponse>('/stock/transfers', payload);
  return data;
}

export async function updateStockTransfer(
  id: number,
  payload: StockTransferRequest,
): Promise<StockTransferResponse> {
  const { data } = await api.put<StockTransferResponse>(`/stock/transfers/${id}`, payload);
  return data;
}

export async function completeStockTransfer(id: number): Promise<StockTransferResponse> {
  const { data } = await api.post<StockTransferResponse>(`/stock/transfers/${id}/complete`);
  return data;
}

export async function cancelStockTransfer(id: number): Promise<StockTransferResponse> {
  const { data } = await api.post<StockTransferResponse>(`/stock/transfers/${id}/cancel`);
  return data;
}

export async function deleteStockTransfer(id: number): Promise<void> {
  await api.delete(`/stock/transfers/${id}`);
}
