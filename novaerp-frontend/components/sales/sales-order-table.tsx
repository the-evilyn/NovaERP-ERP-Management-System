"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  EyeIcon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/providers/auth-provider";
import { useConfirmSaleOrder, useSaleOrders } from "@/hooks/use-sales";
import { formatCurrency, formatDate } from "@/lib/formatters";
import type { SaleOrderResponse, SaleOrderStatus } from "@/types/models";
import { ViewSalesOrderDialog } from "@/components/sales/view-sales-order-dialog";

export function SalesOrderTable(): React.ReactElement {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<SaleOrderStatus | undefined>(undefined);
  const [selectedOrder, setSelectedOrder] = useState<SaleOrderResponse | null>(null);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);

  const { data, isLoading, isError } = useSaleOrders(page, 10, statusFilter);
  const confirmMutation = useConfirmSaleOrder();

  const isAdmin = user?.role === "ADMIN";
  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handleViewOrder = (order: SaleOrderResponse) => {
    setSelectedOrder(order);
    setViewDialogOpen(true);
  };

  const handleQuickConfirm = async (orderId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await confirmMutation.mutateAsync(orderId);
    } catch {
      // Handled in dialog or error display
    }
  };

  const getStatusBadge = (status: SaleOrderStatus) => {
    switch (status) {
      case "DRAFT":
        return <Badge variant="secondary">Brouillon</Badge>;
      case "CONFIRMED":
        return <Badge className="bg-emerald-600 text-white hover:bg-emerald-700">Confirmée</Badge>;
      case "DELIVERED":
        return <Badge className="bg-blue-600 text-white hover:bg-blue-700">Livrée</Badge>;
      case "CANCELLED":
        return <Badge variant="destructive">Annulée</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-4">
      {/* Status Filter Tabs */}
      <div className="flex flex-wrap gap-2 items-center justify-between">
        <div className="flex flex-wrap gap-1 bg-muted/40 p-1 rounded-lg text-xs">
          <Button
            variant={statusFilter === undefined ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter(undefined);
              setPage(0);
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
              setPage(0);
            }}
          >
            Brouillons
          </Button>
          <Button
            variant={statusFilter === "CONFIRMED" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("CONFIRMED");
              setPage(0);
            }}
          >
            Confirmées
          </Button>
          <Button
            variant={statusFilter === "DELIVERED" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("DELIVERED");
              setPage(0);
            }}
          >
            Livrées
          </Button>
          <Button
            variant={statusFilter === "CANCELLED" ? "default" : "ghost"}
            size="sm"
            className="h-7 text-xs"
            onClick={() => {
              setStatusFilter("CANCELLED");
              setPage(0);
            }}
          >
            Annulées
          </Button>
        </div>

        <span className="text-xs text-muted-foreground">
          {totalElements} commande{totalElements > 1 ? "s" : ""} trouvée{totalElements > 1 ? "s" : ""}
        </span>
      </div>

      {/* Orders Table */}
      <div className="border rounded-lg bg-card overflow-x-auto shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground border-b">
            <tr>
              <th className="p-3 text-left">N° Commande</th>
              <th className="p-3 text-left">Client</th>
              <th className="p-3 text-left">Date</th>
              <th className="p-3 text-center">Articles</th>
              <th className="p-3 text-right">Total HT</th>
              <th className="p-3 text-right">Total TTC</th>
              <th className="p-3 text-center">Statut</th>
              <th className="p-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y text-xs">
            {isLoading ? (
              <tr>
                <td colSpan={8} className="p-8 text-center text-muted-foreground">
                  Chargement des commandes de vente...
                </td>
              </tr>
            ) : isError ? (
              <tr>
                <td colSpan={8} className="p-8 text-center text-destructive">
                  <div className="flex items-center justify-center gap-2">
                    <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                    Erreur lors de la récupération des commandes.
                  </div>
                </td>
              </tr>
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={8} className="p-12 text-center text-muted-foreground">
                  <div className="space-y-1">
                    <p className="text-sm font-medium">Aucune commande de vente</p>
                    <p className="text-xs text-muted-foreground">
                      Créez votre première commande pour lancer le flux de commercialisation et la déduction automatique du stock.
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr
                  key={order.id}
                  className="hover:bg-muted/20 cursor-pointer transition-colors"
                  onClick={() => handleViewOrder(order)}
                >
                  <td className="p-3 font-semibold text-primary">
                    {order.orderNumber}
                  </td>
                  <td className="p-3">
                    <div className="font-medium text-foreground">{order.clientName}</div>
                    {order.clientCity && (
                      <div className="text-[11px] text-muted-foreground">
                        {order.clientCity}
                      </div>
                    )}
                  </td>
                  <td className="p-3 text-muted-foreground">
                    {formatDate(order.createdAt)}
                  </td>
                  <td className="p-3 text-center font-medium">
                    {order.items?.length ?? 0}
                  </td>
                  <td className="p-3 text-right font-medium">
                    {formatCurrency(order.subtotalHt)}
                  </td>
                  <td className="p-3 text-right font-bold text-foreground">
                    {formatCurrency(order.totalTtc)}
                  </td>
                  <td className="p-3 text-center">
                    {getStatusBadge(order.status)}
                  </td>
                  <td className="p-3 text-right">
                    <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 gap-1 text-xs"
                        onClick={() => handleViewOrder(order)}
                      >
                        <HugeiconsIcon icon={EyeIcon} className="size-3.5" />
                        Détails
                      </Button>
                      {isAdmin && order.status === "DRAFT" && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 text-xs gap-1 text-emerald-700 border-emerald-300 hover:bg-emerald-50"
                          disabled={confirmMutation.isPending}
                          onClick={(e) => handleQuickConfirm(order.id, e)}
                        >
                          <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3.5" />
                          Confirmer
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

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted-foreground pt-2">
          <span>
            Page {page + 1} sur {totalPages}
          </span>
          <div className="flex gap-1">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Précédent
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Suivant
            </Button>
          </div>
        </div>
      )}

      {/* Order Details Modal */}
      <ViewSalesOrderDialog
        order={selectedOrder}
        open={viewDialogOpen}
        onOpenChange={setViewDialogOpen}
      />
    </div>
  );
}
