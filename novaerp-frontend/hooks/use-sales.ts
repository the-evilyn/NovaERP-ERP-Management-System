import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelSaleOrder,
  confirmSaleOrder,
  createSaleOrder,
  getSaleOrder,
  getSaleOrders,
  updateSaleOrder,
} from "@/services/sales.service";
import type { SaleOrderRequest, SaleOrderStatus } from "@/types/models";

export function useSaleOrders(
  page = 0,
  size = 10,
  status?: SaleOrderStatus,
  clientId?: number,
) {
  return useQuery({
    queryKey: ["sales-orders", page, size, status, clientId],
    queryFn: () => getSaleOrders(page, size, status, clientId),
  });
}

export function useSaleOrder(id?: number) {
  return useQuery({
    queryKey: ["sales-orders", id],
    queryFn: () => (id ? getSaleOrder(id) : null),
    enabled: typeof id === "number" && Number.isFinite(id),
  });
}

export function useCreateSaleOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SaleOrderRequest) => createSaleOrder(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-orders"] });
    },
  });
}

export function useUpdateSaleOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: SaleOrderRequest }) =>
      updateSaleOrder(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-orders"] });
    },
  });
}

export function useConfirmSaleOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => confirmSaleOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-orders"] });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-kpis"] });
    },
  });
}

export function useCancelSaleOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => cancelSaleOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales-orders"] });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-kpis"] });
    },
  });
}
