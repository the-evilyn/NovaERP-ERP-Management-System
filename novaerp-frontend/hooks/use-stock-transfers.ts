import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelStockTransfer,
  completeStockTransfer,
  createStockTransfer,
  deleteStockTransfer,
  getStockTransfer,
  getStockTransfers,
  updateStockTransfer,
} from "@/services/stock-transfers.service";
import type { StockTransferRequest, StockTransferStatus } from "@/types/models";

export function useStockTransfers(
  page = 0,
  size = 10,
  status?: StockTransferStatus,
  warehouseId?: number,
) {
  return useQuery({
    queryKey: ["stock-transfers", page, size, status, warehouseId],
    queryFn: () => getStockTransfers(page, size, status, warehouseId),
  });
}

export function useStockTransfer(id?: number) {
  return useQuery({
    queryKey: ["stock-transfers", id],
    queryFn: () => (id ? getStockTransfer(id) : null),
    enabled: typeof id === "number" && Number.isFinite(id) && id > 0,
  });
}

export function useCreateStockTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: StockTransferRequest) => createStockTransfer(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-transfers"] });
    },
  });
}

export function useUpdateStockTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: StockTransferRequest }) =>
      updateStockTransfer(id, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["stock-transfers"] });
      queryClient.invalidateQueries({ queryKey: ["stock-transfers", variables.id] });
    },
  });
}

export function useCompleteStockTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => completeStockTransfer(id),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ["stock-transfers"] });
      queryClient.invalidateQueries({ queryKey: ["stock-transfers", id] });
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
      queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-kpis"] });
    },
  });
}

export function useCancelStockTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => cancelStockTransfer(id),
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ["stock-transfers"] });
      queryClient.invalidateQueries({ queryKey: ["stock-transfers", id] });
    },
  });
}

export function useDeleteStockTransfer() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteStockTransfer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-transfers"] });
    },
  });
}
