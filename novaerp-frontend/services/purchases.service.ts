// services/purchases.service.ts
// API service for Purchase Orders (/api/purchases/orders)

import { api } from '@/lib/axios';
import type {
  Page,
  PurchaseOrderRequest,
  PurchaseOrderResponse,
  PurchaseOrderStatus,
} from '@/types/models';

export async function getPurchaseOrders(
  page = 0,
  size = 10,
  status?: PurchaseOrderStatus,
  supplierId?: number,
): Promise<Page<PurchaseOrderResponse>> {
  const { data } = await api.get<Page<PurchaseOrderResponse>>('/purchases/orders', {
    params: {
      page,
      size,
      status: status || undefined,
      supplierId: supplierId || undefined,
    },
  });
  return data;
}

export async function getPurchaseOrder(id: number): Promise<PurchaseOrderResponse> {
  const { data } = await api.get<PurchaseOrderResponse>(`/purchases/orders/${id}`);
  return data;
}

export async function createPurchaseOrder(
  payload: PurchaseOrderRequest,
): Promise<PurchaseOrderResponse> {
  const { data } = await api.post<PurchaseOrderResponse>('/purchases/orders', payload);
  return data;
}

export async function updatePurchaseOrder(
  id: number,
  payload: PurchaseOrderRequest,
): Promise<PurchaseOrderResponse> {
  const { data } = await api.put<PurchaseOrderResponse>(`/purchases/orders/${id}`, payload);
  return data;
}

export async function confirmPurchaseOrder(id: number): Promise<PurchaseOrderResponse> {
  const { data } = await api.post<PurchaseOrderResponse>(`/purchases/orders/${id}/confirm`);
  return data;
}

export async function receivePurchaseOrder(id: number): Promise<PurchaseOrderResponse> {
  const { data } = await api.post<PurchaseOrderResponse>(`/purchases/orders/${id}/receive`);
  return data;
}

export async function cancelPurchaseOrder(id: number): Promise<PurchaseOrderResponse> {
  const { data } = await api.post<PurchaseOrderResponse>(`/purchases/orders/${id}/cancel`);
  return data;
}
