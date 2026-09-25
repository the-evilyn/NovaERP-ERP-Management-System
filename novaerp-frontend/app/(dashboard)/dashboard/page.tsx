"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AiBrain01Icon,
  AlertDiamondIcon,
  Exchange02Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import type React from "react";
import { useState } from "react";
import { cn } from "@/lib/utils";
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
import { useAuth } from "@/providers/auth-provider";
import { QuickReorderDialog } from "@/components/decision/quick-reorder-dialog";
import { useDashboardStats, useSalesEvolution } from "@/hooks/use-dashboard";
import { useRecommendations, useRiskSummary } from "@/hooks/use-decision";
import { useAllStockMovements } from "@/hooks/use-stock-movements";
import type {
  ReorderRecommendationResponse,
  RiskLevel,
  StockMovementType,
} from "@/types/models";
import { StockValueByCategoryChart } from "@/components/dashboard/stock-value-by-category-chart";
import { SalesEvolutionChart } from "@/components/dashboard/sales-evolution-chart";
import { formatCurrency, formatDateTime } from "@/lib/formatters";

const movementBadgeVariant: Record<
  StockMovementType,
  "success" | "destructive" | "warning"
> = {
  IN: "success",
  OUT: "destructive",
  ADJUSTMENT: "warning",
};

const movementLabel: Record<StockMovementType, string> = {
  IN: "Entrée",
  OUT: "Sortie",
  ADJUSTMENT: "Ajustement",
};

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
    case "INACTIVE":
      return (
        <Badge variant="outline" className="text-muted-foreground border-dashed">
          Inactif
        </Badge>
      );
    case "LOW":
    case "NORMAL":
    default:
      return <Badge variant="success">Faible</Badge>;
  }
}

function StatItem({
  title,
  value,
  loading,
  last,
}: {
  title: string;
  value: string;
  loading: boolean;
  last?: boolean;
}): React.ReactElement {
  return (
    <div
      className={cn(
        "w-full border-border border-b pb-8 last:border-b-0 sm:border-b-0 sm:border-r sm:pb-0",
        last && "sm:border-r-0",
      )}
    >
      {loading ? (
        <Skeleton className="mx-auto h-11 w-24" />
      ) : (
        <div className="text-center font-heading font-bold text-4xl text-foreground sm:text-5xl">
          {value}
        </div>
      )}
      <span className="mt-3 block text-center text-muted-foreground text-sm sm:text-base">
        {title}
      </span>
    </div>
  );
}

export default function DashboardPage(): React.ReactElement {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [selectedRecommendation, setSelectedRecommendation] =
    useState<ReorderRecommendationResponse | null>(null);

  const { data: stats, isPending: isStatsPending } = useDashboardStats();
  const { data: riskSummary, isPending: isRiskSummaryPending } = useRiskSummary();
  const { data: salesEvolution, isPending: isSalesPending } = useSalesEvolution(6);
  const { data: recommendationsPage, isPending: isRecsPending } =
    useRecommendations("ALL", 0, 5);
  const { data: movementsPage, isPending: isMovementsPending } =
    useAllStockMovements(0, 6);

  const categoryValues = stats?.categoryValues ?? [];
  const topArticles = stats?.topArticles ?? [];

  return (
    <div className="flex flex-col gap-6">
      {/* Role-Specific Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isAdmin ? "Tableau de bord Administrateur" : "Tableau de bord Opérateur"}
          </h1>
          <p className="text-sm text-muted-foreground">
            {isAdmin
              ? "Supervision globale de l'activité, valorisation du stock et pilotage des réapprovisionnements."
              : "Suivi opérationnel des stocks, alertes de réapprovisionnement et traçabilité des mouvements."}
          </p>
        </div>
        <Badge variant={isAdmin ? "default" : "secondary"} className="self-start sm:self-auto">
          {isAdmin ? "Espace Administrateur" : "Espace Opérateur de Stock"}
        </Badge>
      </div>

      {/* Inventory Health KPIs */}
      <div className="flex flex-col gap-8 rounded-2xl border bg-card p-6 sm:flex-row sm:justify-between sm:gap-0 sm:p-10">
        {isAdmin ? (
          <StatItem
            title="Valeur totale du stock"
            value={formatCurrency(stats?.totalValue ?? 0)}
            loading={isStatsPending}
          />
        ) : (
          <StatItem
            title="Articles en alerte"
            value={String(riskSummary?.totalArticlesAtRisk ?? 0)}
            loading={isRiskSummaryPending}
          />
        )}
        <StatItem
          title="Stock critique"
          value={String(riskSummary?.criticalCount ?? 0)}
          loading={isRiskSummaryPending}
        />
        <StatItem
          title="Stock faible"
          value={String(
            (riskSummary?.highCount ?? 0) + (riskSummary?.mediumCount ?? 0)
          )}
          loading={isRiskSummaryPending}
        />
        <StatItem
          title="Rupture de stock"
          value={String(riskSummary?.outOfStockCount ?? 0)}
          loading={isRiskSummaryPending}
          last
        />
      </div>

      {/* Catalog Counts */}
      <div className="flex flex-col gap-8 rounded-2xl border bg-card p-6 sm:flex-row sm:justify-between sm:gap-0 sm:p-10">
        <StatItem
          title="Articles"
          value={String(stats?.totalArticles ?? 0)}
          loading={isStatsPending}
        />
        <StatItem
          title="Catégories"
          value={String(stats?.totalCategories ?? 0)}
          loading={isStatsPending}
        />
        <StatItem
          title="Fournisseurs"
          value={String(stats?.totalSuppliers ?? 0)}
          loading={isStatsPending}
        />
        <StatItem
          title={isAdmin ? "Clients" : "Quantité totale"}
          value={String(isAdmin ? (stats?.totalClients ?? 0) : (stats?.totalQuantity ?? 0))}
          loading={isStatsPending}
          last
        />
      </div>

      {/* Smart Stock Intelligence Alert Banner */}
      {recommendationsPage && recommendationsPage.content.some((r) => r.riskLevel === "CRITICAL" && r.suggestedQuantity > 0) && (
        <div className="flex flex-col gap-3 rounded-xl border border-destructive/30 bg-destructive/8 p-4 text-sm text-destructive-foreground sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-destructive/15 p-2 text-destructive shrink-0">
              <HugeiconsIcon icon={AlertDiamondIcon} size={20} strokeWidth={2} />
            </div>
            <div>
              <span className="font-semibold">Alerte Réapprovisionnement Critique :</span>{" "}
              Des articles sont en rupture immédiate ou ont une couverture inférieure au délai fournisseur. Une commande urgente est recommandée.
            </div>
          </div>
          <Button variant="outline" size="sm" render={<Link href="/decisions" />}>
            Traiter les alertes
          </Button>
        </div>
      )}

      {/* Decision Support Reorder Alert Card */}
      <CardFrame className="border-amber-500/20 bg-gradient-to-r from-amber-500/[0.03] to-transparent">
        <CardFrameHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-amber-500/10 p-2 text-amber-600 dark:text-amber-400">
              <HugeiconsIcon icon={AiBrain01Icon} size={22} strokeWidth={2} />
            </div>
            <div>
              <CardFrameTitle>
                Aide à la Décision : Alertes Prioritaires de Réapprovisionnement
              </CardFrameTitle>
              <CardFrameDescription>
                Articles prioritaires nécessitant une commande selon la consommation réelle et les délais fournisseurs
              </CardFrameDescription>
            </div>
          </div>
          <Button variant="outline" size="sm" render={<Link href="/decisions" />}>
            Voir tout le plan ({recommendationsPage?.totalElements ?? 0})
          </Button>
        </CardFrameHeader>

        <Table variant="card">
          <TableHeader>
            <TableRow>
              <TableHead>Article</TableHead>
              <TableHead>Stock / Min</TableHead>
              <TableHead className="text-right">Conso moy.</TableHead>
              <TableHead className="text-right">Couverture</TableHead>
              <TableHead>Risque</TableHead>
              <TableHead className="text-right">Qté suggérée</TableHead>
              <TableHead>Fournisseur optimal</TableHead>
              {isAdmin && <TableHead className="text-right">Budget estimé</TableHead>}
              {isAdmin && <TableHead className="text-right">Action</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isRecsPending &&
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={isAdmin ? 9 : 7}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isRecsPending && recommendationsPage?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 9 : 7} className="h-20 text-center text-muted-foreground">
                  Aucun article en rupture ou critique détecté. Le stock est optimal.
                </TableCell>
              </TableRow>
            )}

            {recommendationsPage?.content.map((rec) => {
              const isInactive = rec.riskLevel === "INACTIVE";
              const hasZeroStock = rec.currentStock <= 0;
              const dsrLabel = isInactive
                ? "Sans demande"
                : hasZeroStock
                  ? "0 j (Rupture)"
                  : rec.daysOfStockRemaining != null
                    ? `${rec.daysOfStockRemaining} j`
                    : "—";

              return (
                <TableRow key={rec.articleId}>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium">{rec.designation}</span>
                      <span className="text-muted-foreground text-xs">
                        Réf: {rec.articleReference}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className={!isInactive && hasZeroStock ? "font-semibold text-destructive" : ""}>
                      {rec.currentStock} / {rec.minStockQuantity}
                    </span>
                  </TableCell>
                  <TableCell className="text-right text-sm">
                    {rec.averageDailyConsumption != null
                      ? `${rec.averageDailyConsumption} /j`
                      : "0 /j"}
                  </TableCell>
                  <TableCell className="text-right text-sm">
                    <span
                      className={
                        !isInactive &&
                        (hasZeroStock ||
                          (rec.daysOfStockRemaining != null &&
                            rec.daysOfStockRemaining <= (rec.leadTimeDays ?? 7)))
                          ? "font-semibold text-destructive"
                          : ""
                      }
                    >
                      {dsrLabel}
                    </span>
                  </TableCell>
                  <TableCell>
                    <RiskBadge level={rec.riskLevel} />
                  </TableCell>
                  <TableCell className="text-right font-semibold">
                    {isInactive ? (
                      <span className="font-normal text-muted-foreground">0</span>
                    ) : (
                      `+${rec.suggestedQuantity} ${rec.unitName ?? ""}`
                    )}
                  </TableCell>
                  <TableCell className="text-sm">
                    {rec.recommendedSupplierName}
                    <span className="ml-1 text-muted-foreground text-xs">
                      ({rec.leadTimeDays != null ? `${rec.leadTimeDays}j` : "7j"})
                    </span>
                  </TableCell>
                  {isAdmin && (
                    <TableCell className="text-right font-medium">
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
              );
            })}
          </TableBody>
        </Table>
      </CardFrame>

      {/* Category Chart & Top Articles (ADMIN Financial Valuation Only) */}
      {isAdmin && (
        <div className="flex flex-col gap-4">
          <CardFrame>
            <CardFrameHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardFrameTitle>Évolution des ventes</CardFrameTitle>
                <CardFrameDescription>
                  Chiffre d&apos;affaires mensuel sur les 6 derniers mois (commandes clients)
                </CardFrameDescription>
              </div>
              <Badge variant="outline" className="self-start sm:self-auto text-xs">
                6 derniers mois
              </Badge>
            </CardFrameHeader>
            <div className="px-6 pb-6">
              {isSalesPending ? (
                <div className="flex h-56 items-center justify-center">
                  <Skeleton className="h-48 w-full" />
                </div>
              ) : (
                <SalesEvolutionChart data={salesEvolution ?? []} />
              )}
            </div>
          </CardFrame>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <CardFrame>
              <CardFrameHeader>
                <CardFrameTitle>Valeur du stock par catégorie</CardFrameTitle>
                <CardFrameDescription>
                  {stats?.totalArticles
                    ? `Agrégation globale sur les ${stats.totalArticles} articles du catalogue`
                    : "Agrégation globale sur les articles du catalogue"}
                </CardFrameDescription>
              </CardFrameHeader>
              <div className="px-6 pb-6">
                {isStatsPending ? (
                  <div className="flex flex-col gap-4">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Skeleton key={i} className="h-8 w-full" />
                    ))}
                  </div>
                ) : (
                  <StockValueByCategoryChart data={categoryValues} />
                )}
              </div>
            </CardFrame>

            <CardFrame>
              <CardFrameHeader>
                <CardFrameTitle>Meilleurs articles</CardFrameTitle>
                <CardFrameDescription>Par valeur de stock totale</CardFrameDescription>
              </CardFrameHeader>
              <Table variant="card">
                <TableHeader>
                  <TableRow>
                    <TableHead>Article</TableHead>
                    <TableHead className="text-right">Valeur</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isStatsPending &&
                    Array.from({ length: 4 }).map((_, i) => (
                      <TableRow key={i}>
                        <TableCell colSpan={2}>
                          <Skeleton className="h-5 w-full" />
                        </TableCell>
                      </TableRow>
                    ))}
                  {topArticles.map((article) => (
                    <TableRow key={article.id}>
                      <TableCell className="font-medium">
                        {article.designation}
                        <span className="ml-1.5 text-muted-foreground text-xs">
                          {article.reference}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge variant="success">
                          {formatCurrency(article.stockValue)}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardFrame>
          </div>
        </div>
      )}

      {/* Recent Movements */}
      <CardFrame className="w-full">
        <CardFrameHeader>
          <CardFrameTitle>Mouvements récents</CardFrameTitle>
          <CardFrameDescription>
            Dernières entrées, sorties et ajustements de stock
          </CardFrameDescription>
        </CardFrameHeader>
        <Table variant="card">
          <TableHeader>
            <TableRow>
              <TableHead>Article</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="text-right">Quantité</TableHead>
              <TableHead>Par</TableHead>
              <TableHead className="text-right">Date</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isMovementsPending &&
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-5 w-full" />
                  </TableCell>
                </TableRow>
              ))}
            {movementsPage?.content.map((movement) => (
              <TableRow key={movement.id}>
                <TableCell className="font-medium">
                  {movement.articleReference}
                </TableCell>
                <TableCell>
                  <Badge variant={movementBadgeVariant[movement.type]}>
                    {movementLabel[movement.type]}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  {movement.quantity}
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {movement.createdByName}
                </TableCell>
                <TableCell className="text-right text-muted-foreground">
                  {formatDateTime(movement.createdAt)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardFrame>

      {/* Modal for Quick Reorder from Dashboard (ADMIN only) */}
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
