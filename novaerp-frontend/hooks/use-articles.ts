import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createArticle,
  createArticleSupplierPrice,
  deleteArticle,
  deleteArticleSupplierPrice,
  getArticle,
  getArticles,
  getArticleSupplierPrices,
  setArticleSupplierPricePrimary,
  updateArticle,
} from "@/services/articles.service";
import type { ArticleRequest, ArticleSupplierPriceRequest } from "@/types/models";

export function useArticles(page = 0, size = 20) {
  return useQuery({
    queryKey: ["articles", page, size],
    queryFn: () => getArticles(page, size),
  });
}

export function useArticle(id: number) {
  return useQuery({
    queryKey: ["articles", id],
    queryFn: () => getArticle(id),
    enabled: Number.isFinite(id),
  });
}

export function useCreateArticle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ArticleRequest) => createArticle(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["articles"] });
    },
  });
}

export function useUpdateArticle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ArticleRequest }) =>
      updateArticle(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["articles"] });
    },
  });
}

export function useDeleteArticle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteArticle(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["articles"] });
    },
  });
}

export function useArticleSupplierPrices(articleId: number) {
  return useQuery({
    queryKey: ["articles", articleId, "supplier-prices"],
    queryFn: () => getArticleSupplierPrices(articleId),
    enabled: Number.isFinite(articleId),
  });
}

export function useCreateArticleSupplierPrice(articleId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ArticleSupplierPriceRequest) =>
      createArticleSupplierPrice(articleId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["articles", articleId, "supplier-prices"],
      });
    },
  });
}

export function useDeleteArticleSupplierPrice(articleId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (priceId: number) =>
      deleteArticleSupplierPrice(articleId, priceId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["articles", articleId, "supplier-prices"],
      });
    },
  });
}

export function useSetPrimarySupplierPrice(articleId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (priceId: number) =>
      setArticleSupplierPricePrimary(articleId, priceId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["articles", articleId, "supplier-prices"],
      });
    },
  });
}
