"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  Download01Icon,
  EyeIcon,
  InboxDownloadIcon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/providers/auth-provider";
import { useConfirmPurchaseOrder, usePurchaseOrders, useReceivePurchaseOrder } from "@/hooks/use-purchases";
import { downloadPdfBlob } from "@/lib/pdf-download";
import { formatCurrency, formatDate } from "@/lib/formatters";
import { downloadPurchaseOrderPdf } from "@/services/purchases.service";
import type { PurchaseOrderResponse, PurchaseOrderStatus } from "@/types/models";
import { ViewPurchaseOrderDialog } from "@/components/purchases/view-purchase-order-dialog";

export function PurchaseOrderTable(): React.ReactElement {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<PurchaseOrderStatus | undefined>(undefined);
  const [selectedOrder, setSelectedOrder] = useState<PurchaseOrderResponse | null>(null);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const { data, isLoading, isError } = usePurchaseOrders(page, 10, statusFilter);
  const confirmMutation = useConfirmPurchaseOrder();
  const receiveMutation = useReceivePurchaseOrder();

  const isAdmin = user?.role === "ADMIN";
  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handleViewOrder = (order: PurchaseOrderResponse) => {
    setSelectedOrder(order);
    setViewDialogOpen(true);
  };

  const handleQuickConfirm = async (orderId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await confirmMutation.mutateAsync(orderId);
    } catch {
      // Handled in dialog or toast
    }
  };

  const handleQuickReceive = async (orderId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await receiveMutation.mutateAsync(orderId);
    } catch {
      // Handled in dialog or toast
    }
  };

  const handleDownloadPdf = async (orderId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    setDownloadingId(orderId);
    try {
      const { blob, filename } = await downloadPurchaseOrderPdf(orderId);
      downloadPdfBlob(blob, filename);
    } catch {
      // Handled gracefully
    } finally {
      setDownloadingId(null);
    }
  };

  const getStatusBadge = (status: PurchaseOrderStatus) => {
    switch (status) {
      case "DRAFT":
        return <Badge variant="secondary">Brouillon</Badge>;
      case "CONFIRMED":
        return <Badge className="bg-amber-600 text-white hover:bg-amber-700">Confirmée</Badge>;
      case "RECEIVED":
        return <Badge className="bg-emerald-600 text-white hover:bg-emerald-700">Réceptionnée</Badge>;
      case "CANCELLED":
        return <Badge variant="destructive">Annulée</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-4">
      {/* Filters Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-card p-3 rounded-lg border">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-muted-foreground">Filtrer par statut :</span>
          <div className="flex flex-wrap gap-1">
            <Button
              variant={statusFilter === undefined ? "default" : "outline"}
              size="xs"
              onClick={() => {
                setStatusFilter(undefined);
                setPage(0);
              }}
            >
              Tous
            </Button>
            <Button
              variant={statusFilter === "DRAFT" ? "default" : "outline"}
              size="xs"
              onClick={() => {
                setStatusFilter("DRAFT");
                setPage(0);
              }}
            >
              Brouillons
            </Button>
            <Button
              variant={statusFilter === "CONFIRMED" ? "default" : "outline"}
              size="xs"
              onClick={() => {
                setStatusFilter("CONFIRMED");
                setPage(0);
              }}
            >
              Confirmées
            </Button>
            <Button
              variant={statusFilter === "RECEIVED" ? "default" : "outline"}
              size="xs"
              onClick={() => {
                setStatusFilter("RECEIVED");
                setPage(0);
              }}
            >
              Réceptionnées
            </Button>
            <Button
              variant={statusFilter === "CANCELLED" ? "default" : "outline"}
              size="xs"
              onClick={() => {
                setStatusFilter("CANCELLED");
                setPage(0);
              }}
            >
              Annulées
            </Button>
          </div>
        </div>

        <span className="text-xs text-muted-foreground">
          {totalElements} commande{totalElements > 1 ? "s" : ""} trouvée{totalElements > 1 ? "s" : ""}
        </span>
      </div>

      {/* Table Container */}
      <div className="border rounded-lg bg-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs uppercase bg-muted/50 border-b text-muted-foreground font-semibold">
              <tr>
                <th scope="col" className="px-4 py-3">N° Commande</th>
                <th scope="col" className="px-4 py-3">Fournisseur</th>
                <th scope="col" className="px-4 py-3">Entrepôt / Emplacement</th>
                <th scope="col" className="px-4 py-3">Date</th>
                <th scope="col" className="px-4 py-3">Articles</th>
                <th scope="col" className="px-4 py-3 text-right">Total HT</th>
                <th scope="col" className="px-4 py-3 text-right">Total TTC</th>
                <th scope="col" className="px-4 py-3 text-center">Statut</th>
                <th scope="col" className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="text-center py-8 text-muted-foreground">
                    Chargement des commandes d&apos;achat...
                  </td>
                </tr>
              ) : isError ? (
                <tr>
                  <td colSpan={9} className="text-center py-8 text-destructive">
                    <div className="flex items-center justify-center gap-2">
                      <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                      <span>Erreur lors du chargement des commandes d&apos;achat.</span>
                    </div>
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-10 text-muted-foreground">
                    Aucune commande d&apos;achat trouvée.
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr
                    key={order.id}
                    onClick={() => handleViewOrder(order)}
                    className="hover:bg-muted/40 cursor-pointer transition-colors"
                  >
                    <td className="px-4 py-3 font-semibold text-foreground font-mono text-xs">
                      {order.orderNumber}
                    </td>
                    <td className="px-4 py-3 font-medium">
                      {order.supplierName}
                    </td>
                    <td className="px-4 py-3 text-xs">
                      {order.warehouseCode ? (
                        <div className="space-y-0.5 min-w-[120px]">
                          <div className="font-medium text-foreground">
                            {order.warehouseCode}{order.warehouseName ? ` — ${order.warehouseName}` : ""}
                          </div>
                          <div className="text-[11px] text-muted-foreground">
                            {order.locationCode
                              ? `${order.locationCode}${order.locationName ? ` — ${order.locationName}` : ""}`
                              : "—"}
                          </div>
                        </div>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">
                      {order.items.length} article{order.items.length > 1 ? "s" : ""}
                    </td>
                    <td className="px-4 py-3 text-right font-medium text-xs">
                      {formatCurrency(order.subtotalHt)}
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-xs text-foreground">
                      {formatCurrency(order.totalTtc)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      {getStatusBadge(order.status)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          disabled={downloadingId === order.id}
                          onClick={(e) => handleDownloadPdf(order.id, e)}
                          title="Télécharger PDF"
                        >
                          <HugeiconsIcon icon={Download01Icon} className="size-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => handleViewOrder(order)}
                          title="Voir détails"
                        >
                          <HugeiconsIcon icon={EyeIcon} className="size-3.5" />
                        </Button>

                        {isAdmin && order.status === "DRAFT" && (
                          <Button
                            size="xs"
                            className="bg-amber-600 hover:bg-amber-700 text-white gap-1 text-[11px] h-7 px-2"
                            onClick={(e) => handleQuickConfirm(order.id, e)}
                            disabled={confirmMutation.isPending}
                          >
                            <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3" />
                            Confirmer
                          </Button>
                        )}

                        {isAdmin && order.status === "CONFIRMED" && (
                          <Button
                            size="xs"
                            className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1 text-[11px] h-7 px-2"
                            onClick={(e) => handleQuickReceive(order.id, e)}
                            disabled={receiveMutation.isPending}
                          >
                            <HugeiconsIcon icon={InboxDownloadIcon} className="size-3" />
                            Réceptionner
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination controls */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t bg-muted/20 text-xs text-muted-foreground">
            <span>Page {page + 1} sur {totalPages}</span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              >
                Précédent
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((prev) => prev + 1)}
              >
                Suivant
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Details / Action Modal */}
      <ViewPurchaseOrderDialog
        order={selectedOrder}
        open={viewDialogOpen}
        onOpenChange={(open) => {
          setViewDialogOpen(open);
          if (!open) setSelectedOrder(null);
        }}
      />
    </div>
  );
}
