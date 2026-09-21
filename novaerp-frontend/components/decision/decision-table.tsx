"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertDiamondIcon,
  CheckmarkCircle02Icon,
  Exchange02Icon,
  EyeIcon,
  HourglassIcon,
  InformationCircleIcon,
  ShoppingCart01Icon,
} from "@hugeicons/core-free-icons";
import React, { Fragment, useState } from "react";
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

function RiskBadge({ level }: { level: RiskLevel }): React.ReactElement {
  switch (level) {
    case "CRITICAL":
    case "OUT_OF_STOCK":
      return <Badge variant="destructive">Critique</Badge>;
    case "HIGH":
      return (
        <Badge variant="error" className="border border-destructive/25 font-semibold">
          Élevé
        </Badge>
      );
    case "MEDIUM":
    case "WARNING":
      return <Badge variant="warning">Moyen</Badge>;
    case "LOW":
    case "NORMAL":
    default:
      return <Badge variant="success">Faible</Badge>;
  }
}

export function DecisionTable(): React.ReactElement {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [activeTab, setActiveTab] = useState<RiskLevel | "ALL">("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);
  const [expandedRowId, setExpandedRowId] = useState<number | null>(null);
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

  const toggleRowExpansion = (articleId: number) => {
    setExpandedRowId((prev) => (prev === articleId ? null : articleId));
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Top Smart Decision KPIs */}
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
            Articles nécessitant une commande selon le point de réapprovisionnement
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
            Stock nul &bull; Rupture constatée en entrepôt
          </p>
        </div>

        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              Risques critiques
            </span>
            <div className="rounded-lg bg-amber-100 p-2 text-amber-600 dark:bg-amber-950/40">
              <HugeiconsIcon icon={InformationCircleIcon} size={20} strokeWidth={2} />
            </div>
          </div>
          <div className="mt-3 font-heading font-bold text-3xl text-amber-600 dark:text-amber-400">
            {isSummaryPending ? (
              <Skeleton className="h-9 w-16" />
            ) : (
              summary?.criticalCount ?? 0
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            Couverture &le; délai de livraison du fournisseur
          </p>
        </div>

        <div className="rounded-2xl border bg-card p-5">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground text-sm">
              {isAdmin ? "Budget total réapprovisionnement" : "Risques élevés"}
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
              summary?.highCount ?? 0
            )}
          </div>
          <p className="mt-1 text-muted-foreground text-xs">
            {isAdmin
              ? "Budget estimé pour reconstituer les stocks de sécurité"
              : "Articles devant être commandés à court terme"}
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
              Moteur décisionnel déterministe basé sur les ventes réelles livrées, les délais fournisseurs et le stock de sécurité dynamique
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
              variant={activeTab === "CRITICAL" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("CRITICAL")}
            >
              Critique ({summary?.criticalCount ?? 0})
            </Button>
            <Button
              variant={activeTab === "HIGH" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("HIGH")}
            >
              Élevé ({summary?.highCount ?? 0})
            </Button>
            <Button
              variant={activeTab === "MEDIUM" ? "default" : "outline"}
              size="sm"
              onClick={() => handleTabChange("MEDIUM")}
            >
              Moyen ({summary?.mediumCount ?? 0})
            </Button>
          </div>
        </CardFrameHeader>

        <Table variant="card">
          <TableHeader>
            <TableRow>
              <TableHead>Article</TableHead>
              <TableHead>Stock / Min</TableHead>
              <TableHead className="text-right">Conso moy.</TableHead>
              <TableHead className="text-right">Couverture</TableHead>
              <TableHead>Fournisseur optimal</TableHead>
              <TableHead>Risque</TableHead>
              <TableHead className="text-right">Qté suggérée</TableHead>
              {isAdmin && <TableHead className="text-right">Budget estimé</TableHead>}
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isPending &&
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={isAdmin ? 9 : 8}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isPending && data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 9 : 8} className="h-32 text-center text-muted-foreground">
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

            {data?.content.map((rec) => {
              const isExpanded = expandedRowId === rec.articleId;
              const hasZeroStock = rec.currentStock <= 0;
              const dsrLabel =
                hasZeroStock
                  ? "0 j (Rupture)"
                  : rec.daysOfStockRemaining != null
                    ? `${rec.daysOfStockRemaining} j`
                    : "— (sans vente)";

              return (
                <Fragment key={rec.articleId}>
                  <TableRow className={isExpanded ? "bg-muted/20" : undefined}>
                    <TableCell>
                      <div className="flex flex-col">
                        <span className="font-medium">{rec.designation}</span>
                        <div className="flex items-center gap-2 text-muted-foreground text-xs">
                          <span>Réf: {rec.articleReference}</span>
                          {rec.categoryName && <span>&bull; {rec.categoryName}</span>}
                        </div>
                      </div>
                    </TableCell>

                    <TableCell>
                      <div className="flex flex-col text-sm">
                        <span className={hasZeroStock ? "font-semibold text-destructive" : ""}>
                          {rec.currentStock} {rec.unitName ?? ""}
                        </span>
                        <span className="text-muted-foreground text-xs">
                          Min: {rec.minStockQuantity}
                        </span>
                      </div>
                    </TableCell>

                    <TableCell className="text-right text-sm">
                      <span className="font-medium">
                        {rec.averageDailyConsumption != null
                          ? `${rec.averageDailyConsumption} /j`
                          : "0 /j"}
                      </span>
                    </TableCell>

                    <TableCell className="text-right text-sm">
                      <span
                        className={
                          hasZeroStock || (rec.daysOfStockRemaining != null && rec.daysOfStockRemaining <= (rec.leadTimeDays ?? 7))
                            ? "font-semibold text-destructive"
                            : ""
                        }
                      >
                        {dsrLabel}
                      </span>
                    </TableCell>

                    <TableCell>
                      <div className="flex flex-col text-sm">
                        <span className="font-medium text-foreground">
                          {rec.recommendedSupplierName}
                        </span>
                        <span className="text-muted-foreground text-xs">
                          Délai : {rec.leadTimeDays != null ? `${rec.leadTimeDays} j` : "7 j (défaut)"} &bull;{" "}
                          {formatCurrency(rec.unitPrice)}/u
                        </span>
                      </div>
                    </TableCell>

                    <TableCell>
                      <RiskBadge level={rec.riskLevel} />
                    </TableCell>

                    <TableCell className="text-right">
                      <span className="font-semibold text-foreground">
                        +{rec.suggestedQuantity}
                      </span>
                      <span className="ml-1 text-muted-foreground text-xs">
                        {rec.unitName ?? ""}
                      </span>
                    </TableCell>

                    {isAdmin && (
                      <TableCell className="text-right font-semibold">
                        {formatCurrency(rec.estimatedBudget)}
                      </TableCell>
                    )}

                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => toggleRowExpansion(rec.articleId)}
                          title="Voir le diagnostic et les explications détaillées"
                        >
                          <HugeiconsIcon icon={EyeIcon} size={16} strokeWidth={2} />
                          <span className="hidden sm:inline">Détails</span>
                        </Button>
                        {isAdmin && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setSelectedRecommendation(rec)}
                          >
                            <HugeiconsIcon icon={Exchange02Icon} strokeWidth={2} />
                            <span className="hidden sm:inline">Commander</span>
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>

                  {/* Expandable Explainability & Decision Calculation Breakdown */}
                  {isExpanded && (
                    <TableRow className="bg-muted/30 hover:bg-muted/30">
                      <TableCell colSpan={isAdmin ? 9 : 8} className="p-4">
                        <div className="rounded-xl border bg-card p-4 space-y-3 shadow-xs">
                          <div className="flex items-start gap-3">
                            <div className="rounded-lg bg-primary/10 p-2 text-primary shrink-0 mt-0.5">
                              <HugeiconsIcon icon={InformationCircleIcon} size={18} strokeWidth={2} />
                            </div>
                            <div className="space-y-1">
                              <h4 className="font-semibold text-sm">
                                Diagnostic &amp; Justification de l&apos;Analyse Décisionnelle
                              </h4>
                              <p className="text-sm text-muted-foreground leading-relaxed">
                                {rec.explanation}
                              </p>
                            </div>
                          </div>

                          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-3 border-t text-xs">
                            <div>
                              <span className="text-muted-foreground">Consommation journalière (ADC) :</span>
                              <p className="font-semibold text-foreground">
                                {rec.averageDailyConsumption != null ? `${rec.averageDailyConsumption} u/jour` : "0 u/jour"}
                              </p>
                            </div>
                            <div>
                              <span className="text-muted-foreground">Délai fournisseur retenu :</span>
                              <p className="font-semibold text-foreground">
                                {rec.leadTimeDays != null ? `${rec.leadTimeDays} jours` : "7 jours (défaut)"}
                              </p>
                            </div>
                            <div>
                              <span className="text-muted-foreground">Demande horizon délai :</span>
                              <p className="font-semibold text-foreground">
                                {rec.expectedNearTermDemand != null ? `${rec.expectedNearTermDemand} unités` : "N/A"}
                              </p>
                            </div>
                            <div>
                              <span className="text-muted-foreground">Point de commande (ROP) :</span>
                              <p className="font-semibold text-foreground">
                                {rec.reorderPoint != null ? `${rec.reorderPoint} unités` : "N/A"}
                              </p>
                            </div>
                          </div>
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              );
            })}
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
