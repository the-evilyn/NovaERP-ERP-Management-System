// services/clients.service.ts
// Real backend calls for the Clients domain (/api/clients).

import { api } from '@/lib/axios';
import type { ClientRequest, ClientResponse, Page } from '@/types/models';

export async function getClients(
  page = 0,
  size = 10,
  search?: string,
): Promise<Page<ClientResponse>> {
  const { data } = await api.get<Page<ClientResponse>>('/clients', {
    params: {
      page,
      size,
      search: search && search.trim() ? search.trim() : undefined,
    },
  });
  return data;
}

export async function getClient(id: number): Promise<ClientResponse> {
  const { data } = await api.get<ClientResponse>(`/clients/${id}`);
  return data;
}

export async function createClient(
  payload: ClientRequest,
): Promise<ClientResponse> {
  const { data } = await api.post<ClientResponse>('/clients', {
    name: payload.name || payload.nom,
    email: payload.email || null,
    phone: payload.phone || payload.telephone || null,
    address: payload.address || payload.adresse || null,
    city: payload.city || null,
    taxNumber: payload.taxNumber || null,
    notes: payload.notes || null,
  });
  return data;
}

export async function updateClient(
  id: number,
  payload: ClientRequest,
): Promise<ClientResponse> {
  const { data } = await api.put<ClientResponse>(`/clients/${id}`, {
    name: payload.name || payload.nom,
    email: payload.email || null,
    phone: payload.phone || payload.telephone || null,
    address: payload.address || payload.adresse || null,
    city: payload.city || null,
    taxNumber: payload.taxNumber || null,
    notes: payload.notes || null,
  });
  return data;
}

export async function deleteClient(id: number): Promise<void> {
  await api.delete(`/clients/${id}`);
}

export async function exportClientsCsv(search?: string): Promise<void> {
  const { data } = await api.get<Blob>('/clients/export', {
    params: {
      search: search && search.trim() ? search.trim() : undefined,
    },
    responseType: 'blob',
  });
  const { downloadCsvBlob } = await import('@/lib/csv');
  downloadCsvBlob(data, 'clients.csv');
}
