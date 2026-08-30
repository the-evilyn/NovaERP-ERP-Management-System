import { api } from '@/lib/axios';
import type { DashboardStatsResponse } from '@/types/models';

export async function getDashboardStats(): Promise<DashboardStatsResponse> {
  const { data } = await api.get<DashboardStatsResponse>('/stock/dashboard/stats');
  return data;
}
