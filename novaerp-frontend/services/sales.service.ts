// services/sales.service.ts
// API service for Sales Orders (/api/sales/orders)

import { api } from '@/lib/axios';
import { extractPdfFilename } from '@/lib/pdf-download';
import type {
  Page,
  SaleOrderRequest,
  SaleOrderResponse,
  SaleOrderStatus,
} from '@/types/models';

export async function getSaleOrders(
  page = 0,
  size = 10,
  status?: SaleOrderStatus,
  clientId?: number,
): Promise<Page<SaleOrderResponse>> {
  const { data } = await api.get<Page<SaleOrderResponse>>('/sales/orders', {
    params: {
      page,
      size,
      status: status || undefined,
      clientId: clientId || undefined,
    },
  });
  return data;
}

export async function getSaleOrder(id: number): Promise<SaleOrderResponse> {
  const { data } = await api.get<SaleOrderResponse>(`/sales/orders/${id}`);
  return data;
}

export async function createSaleOrder(
  payload: SaleOrderRequest,
): Promise<SaleOrderResponse> {
  const { data } = await api.post<SaleOrderResponse>('/sales/orders', payload);
  return data;
}

export async function updateSaleOrder(
  id: number,
  payload: SaleOrderRequest,
): Promise<SaleOrderResponse> {
  const { data } = await api.put<SaleOrderResponse>(`/sales/orders/${id}`, payload);
  return data;
}

export async function confirmSaleOrder(id: number): Promise<SaleOrderResponse> {
  const { data } = await api.post<SaleOrderResponse>(`/sales/orders/${id}/confirm`);
  return data;
}

export async function deliverSaleOrder(id: number): Promise<SaleOrderResponse> {
  const { data } = await api.post<SaleOrderResponse>(`/sales/orders/${id}/deliver`);
  return data;
}

export async function cancelSaleOrder(id: number): Promise<SaleOrderResponse> {
  const { data } = await api.post<SaleOrderResponse>(`/sales/orders/${id}/cancel`);
  return data;
}

export async function downloadSaleOrderPdf(id: number): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get<Blob>(`/sales/orders/${id}/pdf`, { responseType: "blob" });
  const filename = extractPdfFilename(response.headers["content-disposition"], `Commande_Client_CMD-${id}.pdf`);
  return { blob: response.data, filename };
}
