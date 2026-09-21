import { api } from '@/lib/axios';
import type { DashboardStatsResponse, MonthlySalesEvolution } from '@/types/models';

export async function getDashboardStats(): Promise<DashboardStatsResponse> {
  const { data } = await api.get<DashboardStatsResponse>('/stock/dashboard/stats');
  return data;
}

export async function getSalesEvolution(months = 6): Promise<MonthlySalesEvolution[]> {
  const { data } = await api.get<MonthlySalesEvolution[]>('/stock/dashboard/sales-evolution', {
    params: { months },
  });
  return data;
}
