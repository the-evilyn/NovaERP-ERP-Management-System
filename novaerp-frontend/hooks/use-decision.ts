import { useQuery } from "@tanstack/react-query";
import {
  getRecommendations,
  getRiskSummary,
} from "@/services/decision.service";
import type { RiskLevel } from "@/types/models";

export function useRecommendations(
  riskLevel?: RiskLevel | "ALL",
  page = 0,
  size = 20,
) {
  return useQuery({
    queryKey: ["decision-recommendations", riskLevel, page, size],
    queryFn: () => getRecommendations(riskLevel, page, size),
  });
}

export function useRiskSummary() {
  return useQuery({
    queryKey: ["decision-summary"],
    queryFn: getRiskSummary,
  });
}
