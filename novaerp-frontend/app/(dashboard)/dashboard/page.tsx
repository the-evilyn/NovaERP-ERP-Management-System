"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AiBrain01Icon,
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
import { useDashboardStats } from "@/hooks/use-dashboard";
import { useRecommendations } from "@/hooks/use-decision";
import { useAllStockMovements } from "@/hooks/use-stock-movements";
import type {
  ReorderRecommendationResponse,
  RiskLevel,
  StockMovementType,
} from "@/types/models";
import { StockValueByCategoryChart } from "@/components/dashboard/stock-value-by-category-chart";
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
      return <Badge variant="success">Normal</Badge>;
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
            value={String(
              (stats?.criticalStock ?? 0) +
                (stats?.lowStock ?? 0) +
                (stats?.outOfStock ?? 0)
            )}
            loading={isStatsPending}
          />
        )}
        <StatItem
          title="Stock critique"
          value={String(stats?.criticalStock ?? 0)}
          loading={isStatsPending}
        />
        <StatItem
          title="Stock faible"
          value={String(stats?.lowStock ?? 0)}
          loading={isStatsPending}
        />
        <StatItem
          title="Rupture de stock"
          value={String(stats?.outOfStock ?? 0)}
          loading={isStatsPending}
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
                Articles critiques nécessitant une commande immédiate pour éviter un arrêt de production
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
              <TableHead>Stock / Seuil</TableHead>
              <TableHead>Niveau de risque</TableHead>
              <TableHead className="text-right">Qté suggérée</TableHead>
              <TableHead>Fournisseur recommandé</TableHead>
              {isAdmin && <TableHead className="text-right">Budget estimé</TableHead>}
              {isAdmin && <TableHead className="text-right">Action</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isRecsPending &&
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={isAdmin ? 7 : 5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {!isRecsPending && recommendationsPage?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={isAdmin ? 7 : 5} className="h-20 text-center text-muted-foreground">
                  Aucun article en rupture ou critique détecté. Le stock est optimal.
                </TableCell>
              </TableRow>
            )}

            {recommendationsPage?.content.map((rec) => (
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
                  <span className={rec.currentStock === 0 ? "font-semibold text-destructive" : ""}>
                    {rec.currentStock} / {rec.minStockQuantity}
                  </span>
                </TableCell>
                <TableCell>
                  <RiskBadge level={rec.riskLevel} score={rec.riskScore} />
                </TableCell>
                <TableCell className="text-right font-semibold">
                  +{rec.suggestedQuantity} {rec.unitName ?? ""}
                </TableCell>
                <TableCell className="text-sm">
                  {rec.recommendedSupplierName}
                  {rec.leadTimeDays && (
                    <span className="ml-1 text-muted-foreground text-xs">
                      ({rec.leadTimeDays}j)
                    </span>
                  )}
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
            ))}
          </TableBody>
        </Table>
      </CardFrame>

      {/* Category Chart & Top Articles (ADMIN Financial Valuation Only) */}
      {isAdmin && (
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
