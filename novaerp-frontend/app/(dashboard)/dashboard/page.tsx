"use client";

import type React from "react";
import { useMemo } from "react";
import { cn } from "@/lib/utils";
import {
  CardFrame,
  CardFrameDescription,
  CardFrameHeader,
  CardFrameTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useArticles } from "@/hooks/use-articles";
import { useCategories } from "@/hooks/use-categories";
import { useSuppliers } from "@/hooks/use-suppliers";
import { useAllStockMovements } from "@/hooks/use-stock-movements";
import type { ArticleResponse, StockMovementType } from "@/types/models";
import {
  StockValueByCategoryChart,
  type CategoryStockValue,
} from "@/components/dashboard/stock-value-by-category-chart";

const currency = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 0,
});

const dateTimeFormat = new Intl.DateTimeFormat("fr-FR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

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
  const { data: articlesPage, isPending: isArticlesPending } = useArticles(
    0,
    500,
  );
  const { data: categoriesPage, isPending: isCategoriesPending } =
    useCategories(0, 1);
  const { data: suppliersPage, isPending: isSuppliersPending } = useSuppliers(
    0,
    1,
  );
  const { data: movementsPage, isPending: isMovementsPending } =
    useAllStockMovements(0, 8);

  const articles = useMemo(
    () => articlesPage?.content ?? [],
    [articlesPage],
  );

  const stats = useMemo(() => {
    let totalValue = 0;
    let critical = 0;
    let faible = 0;
    let rupture = 0;

    for (const article of articles) {
      totalValue += article.stockQuantity * article.purchasePriceHt;

      if (article.stockQuantity === 0) {
        rupture += 1;
      } else if (article.stockQuantity <= article.minStockQuantity / 3) {
        critical += 1;
      } else if (article.stockQuantity <= article.minStockQuantity) {
        faible += 1;
      }
    }

    return { totalValue, critical, faible, rupture };
  }, [articles]);

  const categoryValues = useMemo<CategoryStockValue[]>(() => {
    const byCategory = new Map<string, number>();

    for (const article of articles) {
      const name = article.categoryName ?? "Sans catégorie";
      const value = article.stockQuantity * article.purchasePriceHt;
      byCategory.set(name, (byCategory.get(name) ?? 0) + value);
    }

    const sorted = Array.from(byCategory.entries())
      .map(([categoryName, value]) => ({ categoryName, value }))
      .sort((a, b) => b.value - a.value);

    if (sorted.length <= 6) {
      return sorted;
    }

    const top = sorted.slice(0, 6);
    const rest = sorted.slice(6);
    const otherValue = rest.reduce((sum, c) => sum + c.value, 0);

    return [...top, { categoryName: "Autres", value: otherValue }];
  }, [articles]);

  const topArticles = useMemo<
    (ArticleResponse & { stockValue: number })[]
  >(() => {
    return [...articles]
      .map((article) => ({
        ...article,
        stockValue: article.stockQuantity * article.purchasePriceHt,
      }))
      .sort((a, b) => b.stockValue - a.stockValue)
      .slice(0, 8);
  }, [articles]);

  const isPending = isArticlesPending;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-8 rounded-2xl border bg-card p-6 sm:flex-row sm:justify-between sm:gap-0 sm:p-10">
        <StatItem
          title="Valeur totale du stock"
          value={currency.format(stats.totalValue)}
          loading={isPending}
        />
        <StatItem
          title="Stock critique"
          value={String(stats.critical)}
          loading={isPending}
        />
        <StatItem
          title="Stock faible"
          value={String(stats.faible)}
          loading={isPending}
        />
        <StatItem
          title="Rupture de stock"
          value={String(stats.rupture)}
          loading={isPending}
          last
        />
      </div>

      <div className="flex flex-col gap-8 rounded-2xl border bg-card p-6 sm:flex-row sm:justify-between sm:gap-0 sm:p-10">
        <StatItem
          title="Articles"
          value={String(articlesPage?.totalElements ?? 0)}
          loading={isArticlesPending}
        />
        <StatItem
          title="Catégories"
          value={String(categoriesPage?.totalElements ?? 0)}
          loading={isCategoriesPending}
        />
        <StatItem
          title="Fournisseurs"
          value={String(suppliersPage?.totalElements ?? 0)}
          loading={isSuppliersPending}
          last
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <CardFrame>
          <CardFrameHeader>
            <CardFrameTitle>Valeur du stock par catégorie</CardFrameTitle>
            <CardFrameDescription>
              Quantité en stock × prix d&apos;achat HT
            </CardFrameDescription>
          </CardFrameHeader>
          <div className="px-6 pb-6">
            {isArticlesPending ? (
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
            <CardFrameDescription>Par valeur de stock</CardFrameDescription>
          </CardFrameHeader>
          <Table variant="card">
            <TableHeader>
              <TableRow>
                <TableHead>Article</TableHead>
                <TableHead className="text-right">Valeur</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isArticlesPending &&
                Array.from({ length: 3 }).map((_, i) => (
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
                      {currency.format(article.stockValue)}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardFrame>
      </div>

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
                  {dateTimeFormat.format(new Date(movement.createdAt))}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardFrame>
    </div>
  );
}
