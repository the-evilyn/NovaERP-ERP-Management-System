// services/categories.service.ts
// Real backend calls for the Categories domain (/api/stock/categories).

import { api } from '@/lib/axios';
import type { CategoryRequest, CategoryResponse, Page } from '@/types/models';

export async function getCategories(
  page = 0,
  size = 20,
): Promise<Page<CategoryResponse>> {
  const { data } = await api.get<Page<CategoryResponse>>('/stock/categories', {
    params: { page, size },
  });
  return data;
}

export async function getCategory(id: number): Promise<CategoryResponse> {
  const { data } = await api.get<CategoryResponse>(`/stock/categories/${id}`);
  return data;
}

export async function createCategory(
  payload: CategoryRequest,
): Promise<CategoryResponse> {
  const { data } = await api.post<CategoryResponse>('/stock/categories', payload);
  return data;
}

export async function updateCategory(
  id: number,
  payload: CategoryRequest,
): Promise<CategoryResponse> {
  const { data } = await api.put<CategoryResponse>(
    `/stock/categories/${id}`,
    payload,
  );
  return data;
}

export async function deleteCategory(id: number): Promise<void> {
  await api.delete(`/stock/categories/${id}`);
}
