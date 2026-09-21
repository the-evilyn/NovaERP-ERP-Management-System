"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertDiamondIcon,
  CheckmarkCircle02Icon,
  Exchange02Icon,
  HourglassIcon,
  InformationCircleIcon,
  ShoppingCart01Icon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  CardFrame,
  CardFrameDescription,
  CardFrameHeader,
  CardFrameTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import PaginationTable from "@/components/shared/pagination-table";
import { useAuth } from "@/providers/auth-provider";
import { QuickReorderDialog } from "@/components/decision/quick-reorder-dialog";
import { useRecommendations, useRiskSummary } from "@/hooks/use-decision";
import { formatCurrency } from "@/lib/formatters";
import type { ReorderRecommendationResponse, RiskLevel } from "@/types/models";

function RiskBadge({
  level,
  score,
}: {
  level: RiskLevel;
  score: number;
}): React.ReactElement {
  switch (level) {
    case "OUT_OF_STOCK":
      return <Badge variant="destructive">Rupture (100%)</Badge>;
    case "CRITICAL":
      return <Badge variant="error">Critique ({Math.round(score)}%)</Badge>;
    case "WARNING":
      return <Badge variant="warning">Faible ({Math.round(score)}%)</Badge>;
    case "NORMAL":
      return <Badge variant="success">Normal (0%)</Badge>;
  }
}

export function DecisionTable(): React.ReactElement {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [activeTab, setActiveTab] = useState<RiskLevel | "ALL">("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);
  const [selectedRecommendation, setSelectedRecommendation] =
    useState<ReorderRecommendationResponse | null>(null);

  const { data: summary, isPending: isSummaryPending } = useRiskSummary();
  const { data, isPending } = useRecommendations(
    activeTab,
    currentPage - 1,
    pageSize,
  );

  const handleTabChange = (tab: RiskLevel | "ALL") => {
    setActiveTab(tab);
    setCurrentPage(1);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Top AI Decision KPIs */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              Articles à réapprovisionner
            </span>
            <div className="rounded-lg bg-red-100 p-2 text-red-600 dark:bg-red-950/40">
              <HugeiconsIcon icon={AlertDiamondIcon} size={20} strokeWidth={2} />
            </div>
          </div>
          <div className="mt-3 font-heading font-bold text-3xl">
            {isSummaryPending ? (
              <Skeleton className="h-9 w-20" />
            ) : (
              summary?.totalArticlesAtRisk ?? 0
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            Articles actuellement sous leur seuil de sécurité
          </p>
        </div>

        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              Ruptures immédiates
            </span>
            <div className="rounded-lg bg-destructive/10 p-2 text-destructive">
              <HugeiconsIcon icon={HourglassIcon} size={20} strokeWidth={2} />
            </div>
          </div>
          <div className="mt-3 font-heading font-bold text-3xl text-destructive">
            {isSummaryPending ? (
              <Skeleton className="h-9 w-20" />
            ) : (
              summary?.outOfStockCount ?? 0
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            Stock nul &bull; Rupture de production critique
          </p>
        </div>

        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              {isAdmin ? "Budget total réapprovisionnement" : "Articles en stock critique"}
            </span>
            <div className="rounded-lg bg-emerald-100 p-2 text-emerald-600 dark:bg-emerald-950/40">
              <HugeiconsIcon icon={ShoppingCart01Icon} size={20} strokeWidth={2} />
            </div>
          </div>
          <div className="mt-3 font-heading font-bold text-3xl text-emerald-600 dark:text-emerald-400">
            {isSummaryPending ? (
              <Skeleton className="h-9 w-32" />
            ) : isAdmin ? (
              formatCurrency(summary?.totalEstimatedReorderBudget ?? 0)
            ) : (
              summary?.criticalCount ?? 0
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            {isAdmin
              ? "Budget estimé pour reconstituer les stocks tampon (+50%)"
              : "Articles prioritaires nécessitant une commande rapide"}
          </p>
        </div>

        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              Score moyen de risque
            </span>
            <div className="rounded-lg bg-amber-100 p-2 text-amber-600 dark:bg-amber-950/40">
              <HugeiconsIcon icon={InformationCircleIcon} size={20} strokeWidth={2} />
            </div>
          </div>
          <div className="mt-3 font-heading font-bold text-3xl text-amber-600 dark:text-amber-400">
            {isSummaryPending ? (
              <Skeleton className="h-9 w-16" />
            ) : (
              `${summary?.averageRiskScore ?? 0}%`
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            Indice de vulnérabilité globale du parc
          </p>
        </div>
      </div>

      {/* Main Table Card */}
      <CardFrame>
        <CardFrameHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardFrameTitle>
              Recommandations Intelligentes de Réapprovisionnement
            </CardFrameTitle>
            <CardFrameDescription>
              Algorithme déterministe basé sur les seuils minimums, les devis fournisseurs et les délais de livraison
            </CardFrameDescription>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              variant={activeTab === "ALL" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("ALL")}
            >
              Tous ({summary?.totalArticlesAtRisk ?? 0})
            </Button>
            <Button
              variant={activeTab === "OUT_OF_STOCK" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("OUT_OF_STOCK")}
            >
              Ruptures ({summary?.outOfStockCount ?? 0})
            </Button>
            <Button
              variant={activeTab === "CRITICAL" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("CRITICAL")}
            >
              Critiques ({summary?.criticalCount ?? 0})
            </Button>
            <Button
              variant={activeTab === "WARNING" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("WARNING")}
            >
              Faibles ({summary?.warningCount ?? 0})
            </Button>
          </div>
        </CardFrameHeader>

        <Table variant="card">
          <TableHeader>
            <TableRow>
              <TableHead>Article</TableHead>
              <TableHead>Niveau de stock</TableHead>
              <TableHead>Niveau de risque</TableHead>
              <TableHead className="text-right">Qté suggérée</TableHead>
              <TableHead>Fournisseur recommandé</TableHead>
              {isAdmin && <TableHead className="text-right">Budget estimé</TableHead>}
              {isAdmin && <TableHead className="text-right">Action</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isPending &&
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={isAdmin ? 7 : 5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isPending && data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 5} className="h-32 text-center text-muted-foreground">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <HugeiconsIcon
                      icon={CheckmarkCircle02Icon}
                      size={32}
                      className="text-emerald-500"
                    />
                    <span>Aucun article ne nécessite de réapprovisionnement dans cette catégorie.</span>
                  </div>
                </TableCell>
              </TableRow>
            )}

            {data?.content.map((rec) => (
              <TableRow key={rec.articleId}>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium">{rec.designation}</span>
                    <div className="flex items-center gap-2 text-muted-foreground text-xs">
                      <span>Réf: {rec.articleReference}</span>
                      {rec.categoryName && (
                        <span>&bull; {rec.categoryName}</span>
                      )}
                    </div>
                  </div>
                </TableCell>

                <TableCell>
                  <div className="flex flex-col text-sm">
                    <span className={rec.currentStock === 0 ? "font-semibold text-destructive" : ""}>
                      {rec.currentStock} {rec.unitName ?? ""}
                    </span>
                    <span className="text-muted-foreground text-xs">
                      Seuil min: {rec.minStockQuantity}
                    </span>
                  </div>
                </TableCell>

                <TableCell>
                  <RiskBadge level={rec.riskLevel} score={rec.riskScore} />
                </TableCell>

                <TableCell className="text-right">
                  <span className="font-semibold text-foreground">
                    +{rec.suggestedQuantity}
                  </span>
                  <span className="ml-1 text-muted-foreground text-xs">
                    {rec.unitName ?? ""}
                  </span>
                </TableCell>

                <TableCell>
                  <div className="flex flex-col text-sm">
                    <span className="font-medium text-foreground">
                      {rec.recommendedSupplierName}
                    </span>
                    <span className="text-muted-foreground text-xs">
                      {rec.leadTimeDays ? `Délai : ${rec.leadTimeDays} j` : "Délai non spécifié"} &bull;{" "}
                      {formatCurrency(rec.unitPrice)}/u
                    </span>
                  </div>
                </TableCell>

                {isAdmin && (
                  <TableCell className="text-right font-semibold">
                    {formatCurrency(rec.estimatedBudget)}
                  </TableCell>
                )}

                {isAdmin && (
                  <TableCell className="text-right">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setSelectedRecommendation(rec)}
                    >
                      <HugeiconsIcon icon={Exchange02Icon} strokeWidth={2} />
                      Commander
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>

        {data && data.totalElements > 0 && (
          <div className="border-t p-4">
            <PaginationTable
              currentPage={currentPage}
              totalPages={data.totalPages}
              pageSize={pageSize}
              totalItems={data.totalElements}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </div>
        )}
      </CardFrame>

      {/* Quick Reorder Modal (ADMIN only) */}
      {isAdmin && (
        <QuickReorderDialog
          recommendation={selectedRecommendation}
          open={selectedRecommendation !== null}
          onOpenChange={(open) => {
            if (!open) setSelectedRecommendation(null);
          }}
        />
      )}
    </div>
  );
}
