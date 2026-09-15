"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { RefreshCwIcon } from "lucide-react";
import type React from "react";
import { useState } from "react";
import PaginationTable from "@/components/shared/pagination-table";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Spinner } from "@/components/ui/spinner";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  useWarehouseLocations,
  useWarehouseStocks,
} from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatQuantity } from "@/lib/formatters";
import { cn } from "@/lib/utils";
import type { WarehouseLocationResponse } from "@/types/models";

export interface WarehouseStockListProps {
  warehouseId: number;
  locations?: WarehouseLocationResponse[];
}

function getStatusBadge(
  quantity: number,
  minQuantity: number | null | undefined,
): React.ReactElement {
  if (quantity <= 0) {
    return (
      <Badge variant="destructive" size="sm">
        Rupture
      </Badge>
    );
  }
  if (
    minQuantity !== null &&
    minQuantity !== undefined &&
    quantity <= minQuantity
  ) {
    return (
      <Badge variant="warning" size="sm">
        Stock faible
      </Badge>
    );
  }
  return (
    <Badge variant="success" size="sm">
      En stock
    </Badge>
  );
}

export function WarehouseStockList({
  warehouseId,
  locations: propsLocations,
}: WarehouseStockListProps): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedLocationId, setSelectedLocationId] = useState<number | null>(
    null,
  );
  const [positiveOnly, setPositiveOnly] = useState(false);

  // Obtain warehouse locations (shared React Query cache with WarehouseLocationsList)
  const locationsQuery = useWarehouseLocations(warehouseId, 0, 100);

  const activeLocations = (
    propsLocations ??
    locationsQuery.data?.content ??
    []
  ).filter((loc) => loc.active);

  const { data, isPending, isFetching, isError, error, refetch } =
    useWarehouseStocks(
      warehouseId,
      currentPage - 1,
      pageSize,
      selectedLocationId,
      positiveOnly,
    );

  const stocks = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  const handleLocationChange = (val: string | null) => {
    const nextLocId = !val || val === "ALL" ? null : Number(val);
    setSelectedLocationId(nextLocId);
    setCurrentPage(1);
  };

  const handlePositiveOnlyChange = (checked: boolean) => {
    setPositiveOnly(checked);
    setCurrentPage(1);
  };

  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
  };

  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage);
  };

  return (
    <div className="flex flex-col gap-3 border-t bg-muted/20 p-4">
      {/* Header & Controls */}
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h4 className="font-semibold text-sm">Stock</h4>
            {data && (
              <Badge variant="outline" size="sm">
                {totalElements}
              </Badge>
            )}
          </div>
          <p className="text-muted-foreground text-xs">
            Visibilité du stock et des niveaux par emplacement
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Location filter */}
          <div className="flex items-center gap-1.5">
            <Select
              value={
                selectedLocationId !== null ? String(selectedLocationId) : "ALL"
              }
              onValueChange={handleLocationChange}
            >
              <SelectTrigger className="h-8 min-w-44 text-xs">
                <SelectValue placeholder="Tous les emplacements" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tous les emplacements</SelectItem>
                {activeLocations.map((loc) => (
                  <SelectItem key={loc.id} value={String(loc.id)}>
                    {loc.code} — {loc.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Positive only switch */}
          <div className="flex items-center gap-2">
            <Switch
              id={`stock-positive-only-${warehouseId}`}
              checked={positiveOnly}
              onCheckedChange={handlePositiveOnlyChange}
            />
            <Label
              htmlFor={`stock-positive-only-${warehouseId}`}
              className="cursor-pointer select-none text-xs font-normal"
            >
              Afficher uniquement stock &gt; 0
            </Label>
          </div>

          {/* Refresh button */}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs"
            onClick={() => refetch()}
            disabled={isFetching}
            title="Actualiser la liste du stock"
          >
            <RefreshCwIcon
              className={cn("size-3.5", isFetching && "animate-spin")}
            />
            <span>Actualiser</span>
          </Button>
        </div>
      </div>

      {/* Error state */}
      {isError && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} />
          <AlertDescription className="flex items-center justify-between gap-2">
            <span>
              {getApiErrorMessage(
                error,
                "Erreur lors de la récupération du stock de l'entrepôt.",
              )}
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-6 px-2 text-xs"
              onClick={() => refetch()}
            >
              Réessayer
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Table */}
      <div className="overflow-x-auto rounded-md border bg-background">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead className="ps-3">Article</TableHead>
              <TableHead>Catégorie</TableHead>
              <TableHead>Emplacement</TableHead>
              <TableHead className="text-right">Quantité</TableHead>
              <TableHead className="text-right">Seuil min</TableHead>
              <TableHead className="text-right">Valeur estimée</TableHead>
              <TableHead className="pe-3 text-center">Statut</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isPending ? (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="h-24 text-center text-muted-foreground text-xs"
                >
                  <div className="flex items-center justify-center gap-2">
                    <Spinner className="size-4" />
                    <span>Chargement du stock...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : stocks.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="h-24 text-center text-muted-foreground text-xs"
                >
                  <div className="flex flex-col items-center justify-center gap-1">
                    <span className="font-medium">Aucun stock disponible</span>
                    <span className="text-muted-foreground text-xs">
                      {selectedLocationId !== null || positiveOnly
                        ? "Aucun article ne correspond aux filtres sélectionnés."
                        : "Cet entrepôt ne contient actuellement aucun article en stock."}
                    </span>
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              stocks.map((item) => (
                <TableRow key={item.id} className="text-xs">
                  <TableCell className="ps-3">
                    <div className="flex flex-col">
                      <span className="font-medium text-foreground">
                        {item.articleDesignation || "—"}
                      </span>
                      <span className="font-mono text-muted-foreground text-xs">
                        {item.articleReference || "—"}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">
                      {item.categoryName || "—"}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-mono font-medium text-primary">
                        {item.locationCode || "—"}
                      </span>
                      {item.locationName && (
                        <span className="text-muted-foreground text-xs">
                          {item.locationName}
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-mono">
                    <span className="font-medium">
                      {formatQuantity(item.quantity, item.unitName)}
                    </span>
                  </TableCell>
                  <TableCell className="text-right font-mono text-muted-foreground">
                    {item.minQuantity !== null && item.minQuantity !== undefined
                      ? formatQuantity(item.minQuantity, item.unitName)
                      : "—"}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {item.totalValueHt !== null && item.totalValueHt !== undefined
                      ? formatCurrency(item.totalValueHt)
                      : "—"}
                  </TableCell>
                  <TableCell className="pe-3 text-center">
                    {getStatusBadge(item.quantity, item.minQuantity)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {data && totalElements > 0 && (
        <PaginationTable
          currentPage={currentPage}
          totalPages={totalPages}
          pageSize={pageSize}
          totalItems={totalElements}
          onPageChange={handlePageChange}
          onPageSizeChange={handlePageSizeChange}
        />
      )}
    </div>
  );
}
