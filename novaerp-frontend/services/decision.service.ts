import { api } from "@/lib/axios";
import type {
  Page,
  ReorderRecommendationResponse,
  RiskLevel,
  StockRiskSummaryResponse,
} from "@/types/models";

export async function getRecommendations(
  riskLevel?: RiskLevel | "ALL",
  page = 0,
  size = 20,
): Promise<Page<ReorderRecommendationResponse>> {
  const params: Record<string, unknown> = { page, size };
  if (riskLevel && riskLevel !== "ALL") {
    params.riskLevel = riskLevel;
  }
  const { data } = await api.get<Page<ReorderRecommendationResponse>>(
    "/stock/decisions",
    { params },
  );
  return data;
}

export async function getRiskSummary(): Promise<StockRiskSummaryResponse> {
  const { data } = await api.get<StockRiskSummaryResponse>(
    "/stock/decisions/summary",
  );
  return data;
}
