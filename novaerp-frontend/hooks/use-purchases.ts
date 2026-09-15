import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelPurchaseOrder,
  confirmPurchaseOrder,
  createPurchaseOrder,
  getPurchaseOrder,
  getPurchaseOrders,
  receivePurchaseOrder,
  updatePurchaseOrder,
} from "@/services/purchases.service";
import type { PurchaseOrderRequest, PurchaseOrderStatus } from "@/types/models";

export function usePurchaseOrders(
  page = 0,
  size = 10,
  status?: PurchaseOrderStatus,
  supplierId?: number,
) {
  return useQuery({
    queryKey: ["purchases-orders", page, size, status, supplierId],
    queryFn: () => getPurchaseOrders(page, size, status, supplierId),
  });
}

export function usePurchaseOrder(id?: number) {
  return useQuery({
    queryKey: ["purchases-orders", id],
    queryFn: () => (id ? getPurchaseOrder(id) : null),
    enabled: typeof id === "number" && Number.isFinite(id),
  });
}

export function useCreatePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: PurchaseOrderRequest) => createPurchaseOrder(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchases-orders"] });
    },
  });
}

export function useUpdatePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PurchaseOrderRequest }) =>
      updatePurchaseOrder(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchases-orders"] });
    },
  });
}

export function useConfirmPurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => confirmPurchaseOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchases-orders"] });
    },
  });
}

export function useReceivePurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => receivePurchaseOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchases-orders"] });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-kpis"] });
      queryClient.invalidateQueries({ queryKey: ["decisions"] });
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
    },
  });
}

export function useCancelPurchaseOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => cancelPurchaseOrder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchases-orders"] });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-kpis"] });
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
    },
  });
}
