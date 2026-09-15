// services/warehouses.service.ts
// Real backend calls for the Warehouses & Locations domain (/api/warehouses).

import { api } from '@/lib/axios';
import type {
  ActiveToggleRequest,
  Page,
  WarehouseLocationRequest,
  WarehouseLocationResponse,
  WarehouseRequest,
  WarehouseResponse,
  WarehouseStockResponse,
} from '@/types/models';

export async function getWarehouses(
  page = 0,
  size = 20,
  active?: boolean,
): Promise<Page<WarehouseResponse>> {
  const params: Record<string, unknown> = { page, size };
  if (typeof active === 'boolean') {
    params.active = active;
  }
  const { data } = await api.get<Page<WarehouseResponse>>('/warehouses', {
    params,
  });
  return data;
}

export async function getWarehouse(id: number): Promise<WarehouseResponse> {
  const { data } = await api.get<WarehouseResponse>(`/warehouses/${id}`);
  return data;
}

export async function createWarehouse(
  payload: WarehouseRequest,
): Promise<WarehouseResponse> {
  const { data } = await api.post<WarehouseResponse>('/warehouses', payload);
  return data;
}

export async function updateWarehouse(
  id: number,
  payload: WarehouseRequest,
): Promise<WarehouseResponse> {
  const { data } = await api.put<WarehouseResponse>(`/warehouses/${id}`, payload);
  return data;
}

export async function setWarehouseActive(
  id: number,
  active: boolean,
): Promise<WarehouseResponse> {
  const body: ActiveToggleRequest = { active };
  const { data } = await api.patch<WarehouseResponse>(
    `/warehouses/${id}/active`,
    body,
  );
  return data;
}

export async function getLocations(
  warehouseId: number,
  page = 0,
  size = 20,
  active?: boolean,
): Promise<Page<WarehouseLocationResponse>> {
  const params: Record<string, unknown> = { page, size };
  if (typeof active === 'boolean') {
    params.active = active;
  }
  const { data } = await api.get<Page<WarehouseLocationResponse>>(
    `/warehouses/${warehouseId}/locations`,
    { params },
  );
  return data;
}

export async function getLocation(
  warehouseId: number,
  locationId: number,
): Promise<WarehouseLocationResponse> {
  const { data } = await api.get<WarehouseLocationResponse>(
    `/warehouses/${warehouseId}/locations/${locationId}`,
  );
  return data;
}

export async function createLocation(
  warehouseId: number,
  payload: WarehouseLocationRequest,
): Promise<WarehouseLocationResponse> {
  const { data } = await api.post<WarehouseLocationResponse>(
    `/warehouses/${warehouseId}/locations`,
    payload,
  );
  return data;
}

export async function updateLocation(
  warehouseId: number,
  locationId: number,
  payload: WarehouseLocationRequest,
): Promise<WarehouseLocationResponse> {
  const { data } = await api.put<WarehouseLocationResponse>(
    `/warehouses/${warehouseId}/locations/${locationId}`,
    payload,
  );
  return data;
}

export async function setLocationActive(
  warehouseId: number,
  locationId: number,
  active: boolean,
): Promise<WarehouseLocationResponse> {
  const body: ActiveToggleRequest = { active };
  const { data } = await api.patch<WarehouseLocationResponse>(
    `/warehouses/${warehouseId}/locations/${locationId}/active`,
    body,
  );
  return data;
}

export async function getWarehouseStocks(
  warehouseId: number,
  params?: {
    locationId?: number;
    positiveOnly?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  },
): Promise<Page<WarehouseStockResponse>> {
  const { data } = await api.get<Page<WarehouseStockResponse>>(
    `/warehouses/${warehouseId}/stocks`,
    { params },
  );
  return data;
}

export async function getArticleWarehouseStocks(
  articleId: number,
): Promise<WarehouseStockResponse[]> {
  const { data } = await api.get<WarehouseStockResponse[]>(
    `/warehouses/stocks/articles/${articleId}`,
  );
  return data;
}
