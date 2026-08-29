import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createStockMovement,
  getAllMovements,
  getArticleMovements,
} from "@/services/stock-movements.service";
import type { StockMovementRequest } from "@/types/models";

export function useArticleMovements(
  articleId: number | null,
  page = 0,
  size = 20,
) {
  return useQuery({
    queryKey: ["stock-movements", articleId, page, size],
    queryFn: () => getArticleMovements(articleId as number, page, size),
    enabled: typeof articleId === "number" && Number.isFinite(articleId),
  });
}

export function useAllStockMovements(page = 0, size = 20) {
  return useQuery({
    queryKey: ["stock-movements", "all", page, size],
    queryFn: () => getAllMovements(page, size),
  });
}

export function useCreateStockMovement() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: StockMovementRequest) => createStockMovement(payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["stock-movements", variables.articleId],
      });
      queryClient.invalidateQueries({ queryKey: ["articles"] });
    },
  });
}
