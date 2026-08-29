// services/suppliers.service.ts
// Real backend calls for the Suppliers domain (/api/stock/suppliers).

import { api } from '@/lib/axios';
import type { Page, SupplierRequest, SupplierResponse } from '@/types/models';

export async function getSuppliers(
  page = 0,
  size = 20,
): Promise<Page<SupplierResponse>> {
  const { data } = await api.get<Page<SupplierResponse>>('/stock/suppliers', {
    params: { page, size },
  });
  return data;
}

export async function getSupplier(id: number): Promise<SupplierResponse> {
  const { data } = await api.get<SupplierResponse>(`/stock/suppliers/${id}`);
  return data;
}

export async function createSupplier(
  payload: SupplierRequest,
): Promise<SupplierResponse> {
  const { data } = await api.post<SupplierResponse>('/stock/suppliers', payload);
  return data;
}

export async function updateSupplier(
  id: number,
  payload: SupplierRequest,
): Promise<SupplierResponse> {
  const { data } = await api.put<SupplierResponse>(
    `/stock/suppliers/${id}`,
    payload,
  );
  return data;
}

export async function deleteSupplier(id: number): Promise<void> {
  await api.delete(`/stock/suppliers/${id}`);
}
