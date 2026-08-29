"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardFrame, CardHeader } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import PaginationTable from "@/components/shared/pagination-table";
import { useArticleMovements } from "@/hooks/use-stock-movements";
import type { ArticleResponse, StockMovementType } from "@/types/models";

const dateFormatter = new Intl.DateTimeFormat("fr-MA", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

const typeLabels: Record<StockMovementType, string> = {
  IN: "Entrée",
  OUT: "Sortie",
  ADJUSTMENT: "Ajustement",
};

function TypeBadge({ type }: { type: StockMovementType }): React.ReactElement {
  if (type === "IN") {
    return <Badge variant="success">{typeLabels[type]}</Badge>;
  }
  if (type === "OUT") {
    return <Badge variant="error">{typeLabels[type]}</Badge>;
  }
  return <Badge variant="info">{typeLabels[type]}</Badge>;
}

export function MovementHistory({
  article,
}: {
  article: ArticleResponse;
}): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);
  }, [article.id]);

  const { data, isPending } = useArticleMovements(
    article.id,
    currentPage - 1,
    pageSize,
  );
  const movements = data?.content;

  return (
    <Card>
      <CardHeader>
        <h2 className="font-semibold text-sm">Historique des mouvements</h2>
        <p className="text-muted-foreground text-xs">
          {article.designation} ({article.reference})
        </p>
      </CardHeader>
      <CardFrame className="mx-6 mb-6">
        <Table variant="card">
          <TableHeader>
            <TableRow>
              <TableHead className="ps-4">Date</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="text-right">Quantité</TableHead>
              <TableHead>Référence</TableHead>
              <TableHead>Note</TableHead>
              <TableHead className="pe-4">Enregistré par</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isPending && (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 px-4 text-center text-muted-foreground"
                >
                  Chargement...
                </TableCell>
              </TableRow>
            )}
            {!isPending && (movements ?? []).length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 px-4 text-center text-muted-foreground"
                >
                  Aucun mouvement enregistré pour cet article.
                </TableCell>
              </TableRow>
            )}
            {(movements ?? []).map((movement) => (
              <TableRow key={movement.id}>
                <TableCell className="ps-4 text-muted-foreground">
                  {dateFormatter.format(new Date(movement.createdAt))}
                </TableCell>
                <TableCell>
                  <TypeBadge type={movement.type} />
                </TableCell>
                <TableCell className="text-right font-medium">
                  {movement.type === "OUT" ? "-" : "+"}
                  {movement.quantity}
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {movement.reference ?? "—"}
                </TableCell>
                <TableCell className="max-w-48 truncate text-muted-foreground">
                  {movement.note ?? "—"}
                </TableCell>
                <TableCell className="pe-4 text-muted-foreground">
                  {movement.createdByName}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardFrame>
      {data && data.totalElements > 0 && (
        <div className="px-6 pb-6">
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
    </Card>
  );
}
