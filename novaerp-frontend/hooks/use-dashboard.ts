import { useQuery } from '@tanstack/react-query';
import { getDashboardStats, getSalesEvolution } from '@/services/dashboard.service';

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: getDashboardStats,
  });
}

export function useSalesEvolution(months = 6) {
  return useQuery({
    queryKey: ['sales-evolution', months],
    queryFn: () => getSalesEvolution(months),
  });
}
