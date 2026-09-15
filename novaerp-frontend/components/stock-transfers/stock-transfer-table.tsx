"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
  AlertCircleIcon,
  ArrowDataTransferHorizontalIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  Delete02Icon,
  EyeIcon,
  MoreVerticalIcon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type React from "react";
import { useState } from "react";
import PaginationTable from "@/components/shared/pagination-table";
import { StockTransferStatusBadge } from "@/components/stock-transfers/stock-transfer-status-badge";
import {
  TransferConfirmationDialog,
  type TransferActionType,
} from "@/components/stock-transfers/transfer-confirmation-dialog";
import { Button } from "@/components/ui/button";
import {
  Menu,
  MenuItem,
  MenuPopup,
  MenuSeparator,
  MenuTrigger,
} from "@/components/ui/menu";
import { Spinner } from "@/components/ui/spinner";
import {
  useCancelStockTransfer,
  useCompleteStockTransfer,
  useDeleteStockTransfer,
  useStockTransfers,
} from "@/hooks/use-stock-transfers";
import { useWarehouses } from "@/hooks/use-warehouses";
import { formatDateTime } from "@/lib/formatters";
import { useAuth } from "@/providers/auth-provider";
import type {
  StockTransferResponse,
  StockTransferStatus,
} from "@/types/models";

export function StockTransferTable(): React.ReactElement {
  const router = useRouter();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<StockTransferStatus | undefined>(undefined);
  const [warehouseFilter, setWarehouseFilter] = useState<number | undefined>(undefined);

  // Dialog state
  const [dialogTransfer, setDialogTransfer] = useState<StockTransferResponse | null>(null);
  const [dialogAction, setDialogAction] = useState<TransferActionType | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const { data, isLoading, isError } = useStockTransfers(
    currentPage - 1,
    pageSize,
    statusFilter,
    warehouseFilter,
  );

  const { data: warehousesData } = useWarehouses(0, 100, true);
  const warehouses = warehousesData?.content ?? [];

  const completeMutation = useCompleteStockTransfer();
  const cancelMutation = useCancelStockTransfer();
  const deleteMutation = useDeleteStockTransfer();

  const transfers = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;

  const openActionDialog = (transfer: StockTransferResponse, action: TransferActionType) => {
    setDialogTransfer(transfer);
    setDialogAction(action);
    setDialogOpen(true);
  };

  const handleConfirmAction = async () => {
    if (!dialogTransfer || !dialogAction) return;

    if (dialogAction === "complete") {
      await completeMutation.mutateAsync(dialogTransfer.id);
    } else if (dialogAction === "cancel") {
      await cancelMutation.mutateAsync(dialogTransfer.id);
    } else if (dialogAction === "delete") {
      await deleteMutation.mutateAsync(dialogTransfer.id);
    }
  };

  return (
    <div className="space-y-4">
      {/* Action Dialog */}
      <TransferConfirmationDialog
        transfer={dialogTransfer}
        action={dialogAction}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onConfirm={handleConfirmAction}
      />

      {/* Filter and Top Bar */}
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        {/* Status Filter Tabs */}
        <div className="flex flex-wrap items-center gap-1 bg-muted/40 p-1 rounded-lg text-xs">
          <Button
            variant={statusFilter === undefined ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter(undefined);
              setCurrentPage(1);
            }}
          >
            Tous ({totalElements})
          </Button>
          <Button
            variant={statusFilter === "DRAFT" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("DRAFT");
              setCurrentPage(1);
            }}
          >
            Brouillons
          </Button>
          <Button
            variant={statusFilter === "COMPLETED" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("COMPLETED");
              setCurrentPage(1);
            }}
          >
            Terminés
          </Button>
          <Button
            variant={statusFilter === "CANCELLED" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("CANCELLED");
              setCurrentPage(1);
            }}
          >
            Annulés
          </Button>
        </div>

        {/* Warehouse Filter & Create Button */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="w-48 sm:w-56">
            <select
              className="flex h-8 w-full rounded-md border border-input bg-background px-2.5 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              value={warehouseFilter ?? ""}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : undefined;
                setWarehouseFilter(val);
                setCurrentPage(1);
              }}
            >
              <option value="">Tous les entrepôts</option>
              {warehouses.map((wh) => (
                <option key={wh.id} value={wh.id}>
                  {wh.code} - {wh.name}
                </option>
              ))}
            </select>
          </div>

          {isAdmin && (
            <Button size="sm" className="h-8 text-xs gap-1.5" render={<Link href="/stock/transfers/new" />}>
              <HugeiconsIcon icon={Add01Icon} className="size-3.5" />
              Nouveau transfert
            </Button>
          )}
        </div>
      </div>

      {/* Transfers Table */}
      <div className="border rounded-lg bg-card overflow-x-auto shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground border-b">
            <tr>
              <th className="p-3 text-left">N° Transfert</th>
              <th className="p-3 text-left">Date de création</th>
              <th className="p-3 text-left">Source</th>
              <th className="p-3 text-left">Destination</th>
              <th className="p-3 text-center">Lignes</th>
              <th className="p-3 text-center">Statut</th>
              <th className="p-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y text-xs">
            {isLoading ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-muted-foreground">
                  <div className="flex items-center justify-center gap-2">
                    <Spinner className="size-4" />
                    Chargement des transferts de stock...
                  </div>
                </td>
              </tr>
            ) : isError ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-destructive">
                  <div className="flex items-center justify-center gap-2">
                    <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                    Erreur lors de la récupération des transferts de stock.
                  </div>
                </td>
              </tr>
            ) : transfers.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-12 text-center text-muted-foreground">
                  <div className="space-y-2">
                    <div className="flex justify-center">
                      <HugeiconsIcon
                        icon={ArrowDataTransferHorizontalIcon}
                        className="size-8 text-muted-foreground/50"
                      />
                    </div>
                    <p className="text-sm font-medium">Aucun transfert de stock trouvé</p>
                    <p className="text-xs text-muted-foreground">
                      {statusFilter || warehouseFilter
                        ? "Modifiez vos filtres pour voir d'autres transferts."
                        : "Créez votre premier transfert entre entrepôts ou emplacements."}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              transfers.map((transfer) => {
                const isDraft = transfer.status === "DRAFT";

                return (
                  <tr
                    key={transfer.id}
                    className="hover:bg-muted/20 transition-colors"
                  >
                    <td className="p-3 font-semibold text-primary">
                      <Link
                        href={`/stock/transfers/${transfer.id}`}
                        className="hover:underline flex items-center gap-1.5"
                      >
                        {transfer.transferNumber}
                      </Link>
                    </td>
                    <td className="p-3 text-muted-foreground">
                      {formatDateTime(transfer.createdAt)}
                    </td>
                    <td className="p-3">
                      <div className="font-medium text-foreground">
                        {transfer.sourceWarehouseCode}
                      </div>
                      <div className="text-[11px] text-muted-foreground">
                        {transfer.sourceLocationCode
                          ? `${transfer.sourceLocationCode}${transfer.sourceLocationName ? ` — ${transfer.sourceLocationName}` : ""}`
                          : "Emplacement par défaut"}
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="font-medium text-foreground">
                        {transfer.destinationWarehouseCode}
                      </div>
                      <div className="text-[11px] text-muted-foreground">
                        {transfer.destinationLocationCode
                          ? `${transfer.destinationLocationCode}${transfer.destinationLocationName ? ` — ${transfer.destinationLocationName}` : ""}`
                          : "Emplacement par défaut"}
                      </div>
                    </td>
                    <td className="p-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-muted text-foreground">
                        {transfer.items?.length ?? 0}
                      </span>
                    </td>
                    <td className="p-3 text-center">
                      <StockTransferStatusBadge status={transfer.status} />
                    </td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          title="Voir détails"
                          onClick={() => router.push(`/stock/transfers/${transfer.id}`)}
                        >
                          <HugeiconsIcon icon={EyeIcon} className="size-3.5" />
                        </Button>

                        {isAdmin && isDraft && (
                          <>
                            <Button
                              variant="ghost"
                              size="icon-xs"
                              title="Modifier"
                              onClick={() => router.push(`/stock/transfers/${transfer.id}/edit`)}
                            >
                              <HugeiconsIcon icon={PencilEdit01Icon} className="size-3.5" />
                            </Button>

                            <Menu>
                              <MenuTrigger render={<Button variant="ghost" size="icon-xs" />}>
                                <HugeiconsIcon icon={MoreVerticalIcon} className="size-3.5" />
                              </MenuTrigger>
                              <MenuPopup align="end" className="w-44 text-xs">
                                <MenuItem
                                  className="gap-2 text-emerald-600 focus:text-emerald-600 focus:bg-emerald-50 dark:focus:bg-emerald-950/40"
                                  onClick={() => openActionDialog(transfer, "complete")}
                                >
                                  <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3.5" />
                                  Valider le transfert
                                </MenuItem>
                                <MenuSeparator />
                                <MenuItem
                                  className="gap-2 text-muted-foreground"
                                  onClick={() => openActionDialog(transfer, "cancel")}
                                >
                                  <HugeiconsIcon icon={Cancel01Icon} className="size-3.5" />
                                  Annuler le transfert
                                </MenuItem>
                                <MenuItem
                                  className="gap-2 text-destructive focus:text-destructive focus:bg-destructive/10"
                                  onClick={() => openActionDialog(transfer, "delete")}
                                >
                                  <HugeiconsIcon icon={Delete02Icon} className="size-3.5" />
                                  Supprimer le brouillon
                                </MenuItem>
                              </MenuPopup>
                            </Menu>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <PaginationTable
          currentPage={currentPage}
          totalPages={totalPages}
          pageSize={pageSize}
          totalItems={totalElements}
          onPageChange={setCurrentPage}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setCurrentPage(1);
          }}
        />
      )}
    </div>
  );
}
