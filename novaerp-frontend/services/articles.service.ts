// services/articles.service.ts
// Real backend calls for the Articles domain (/api/stock/articles) and
// their supplier price quotes (/api/stock/articles/{id}/supplier-prices).

import { api } from '@/lib/axios';
import type {
  ArticleRequest,
  ArticleResponse,
  ArticleSupplierPriceRequest,
  ArticleSupplierPriceResponse,
  Page,
} from '@/types/models';

export async function getArticles(
  page = 0,
  size = 20,
): Promise<Page<ArticleResponse>> {
  const { data } = await api.get<Page<ArticleResponse>>('/stock/articles', {
    params: { page, size },
  });
  return data;
}

export async function getArticle(id: number): Promise<ArticleResponse> {
  const { data } = await api.get<ArticleResponse>(`/stock/articles/${id}`);
  return data;
}

export async function createArticle(
  payload: ArticleRequest,
): Promise<ArticleResponse> {
  const { data } = await api.post<ArticleResponse>('/stock/articles', payload);
  return data;
}

export async function updateArticle(
  id: number,
  payload: ArticleRequest,
): Promise<ArticleResponse> {
  const { data } = await api.put<ArticleResponse>(
    `/stock/articles/${id}`,
    payload,
  );
  return data;
}

export async function deleteArticle(id: number): Promise<void> {
  await api.delete(`/stock/articles/${id}`);
}

export async function getArticleSupplierPrices(
  articleId: number,
): Promise<ArticleSupplierPriceResponse[]> {
  const { data } = await api.get<ArticleSupplierPriceResponse[]>(
    `/stock/articles/${articleId}/supplier-prices`,
  );
  return data;
}

export async function createArticleSupplierPrice(
  articleId: number,
  payload: ArticleSupplierPriceRequest,
): Promise<ArticleSupplierPriceResponse> {
  const { data } = await api.post<ArticleSupplierPriceResponse>(
    `/stock/articles/${articleId}/supplier-prices`,
    payload,
  );
  return data;
}

export async function deleteArticleSupplierPrice(
  articleId: number,
  priceId: number,
): Promise<void> {
  await api.delete(`/stock/articles/${articleId}/supplier-prices/${priceId}`);
}

export async function setArticleSupplierPricePrimary(
  articleId: number,
  priceId: number,
): Promise<ArticleSupplierPriceResponse> {
  const { data } = await api.put<ArticleSupplierPriceResponse>(
    `/stock/articles/${articleId}/supplier-prices/${priceId}/primary`,
  );
  return data;
}
