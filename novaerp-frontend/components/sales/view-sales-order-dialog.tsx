"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogFooter,
  DialogHeader,
  DialogPanel,
  DialogPopup,
  DialogTitle,
} from "@/components/ui/dialog";
import { useCustomerInvoices, useCreateCustomerInvoice } from "@/hooks/use-invoices";
import { useAuth } from "@/providers/auth-provider";
import { useCancelSaleOrder, useConfirmSaleOrder } from "@/hooks/use-sales";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatDateTime, formatQuantity } from "@/lib/formatters";
import type { SaleOrderResponse, SaleOrderStatus } from "@/types/models";

interface ViewSalesOrderDialogProps {
  order: SaleOrderResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ViewSalesOrderDialog({
  order,
  open,
  onOpenChange,
}: ViewSalesOrderDialogProps): React.ReactElement {
  const { user } = useAuth();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const confirmMutation = useConfirmSaleOrder();
  const cancelMutation = useCancelSaleOrder();
  const createInvoiceMutation = useCreateCustomerInvoice();
  const customerInvoicesQuery = useCustomerInvoices(0, 20, undefined, undefined, order?.id);

  const isAdmin = user?.role === "ADMIN";

  if (!order) return <></>;

  const existingCustomerInvoices = customerInvoicesQuery.data?.content ?? [];
  const existingCustomerInvoiceCount = existingCustomerInvoices.length;

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

  const handleConfirm = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await confirmMutation.mutateAsync(order.id);
      setActionSuccess("Commande confirmée avec succès ! Le stock a été mis à jour automatiquement (Mouvement OUT).");
      setTimeout(() => onOpenChange(false), 1500);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err));
    }
  };

  const handleCancel = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await cancelMutation.mutateAsync(order.id);
      setActionSuccess(
        order.status === "CONFIRMED"
          ? "Commande annulée ! Le stock a été réintégré (Mouvement IN)."
          : "Commande annulée."
      );
      setTimeout(() => onOpenChange(false), 1500);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err));
    }
  };

  const handleCreateInvoice = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await createInvoiceMutation.mutateAsync({
        clientId: order.clientId,
        saleOrderId: order.id,
        items: order.items.map((item) => ({
          articleId: item.articleId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          taxRate: item.taxRate,
        })),
        taxRate: order.taxRate,
        notes: order.notes ?? undefined,
      });
      setActionSuccess("Facture client créée à partir de cette commande.");
      setTimeout(() => onOpenChange(false), 1500);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Impossible de créer la facture client."));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader className="flex flex-row items-center justify-between pb-2">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <DialogTitle className="text-xl font-bold">
                Commande {order.orderNumber}
              </DialogTitle>
              {getStatusBadge(order.status)}
            </div>
            <p className="text-xs text-muted-foreground">
              Créée le {formatDateTime(order.createdAt)}
              {order.createdByName ? ` par ${order.createdByName}` : ""}
            </p>
          </div>
        </DialogHeader>

        <DialogPanel className="space-y-4 pt-2">
          {errorMsg && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
              <AlertDescription>{errorMsg}</AlertDescription>
            </Alert>
          )}

          {actionSuccess && (
            <Alert className="border-emerald-500 bg-emerald-50 text-emerald-800">
              <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4 text-emerald-600" />
              <AlertDescription>{actionSuccess}</AlertDescription>
            </Alert>
          )}

          {/* Client & Metadata Card */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 bg-muted/40 p-3 rounded-lg text-xs">
            <div>
              <span className="text-muted-foreground block">Client:</span>
              <span className="font-semibold text-sm">{order.clientName}</span>
            </div>
            <div>
              <span className="text-muted-foreground block">Ville:</span>
              <span className="font-medium">{order.clientCity || "Non précisée"}</span>
            </div>
            <div>
              <span className="text-muted-foreground block">Date confirmation:</span>
              <span className="font-medium">
                {order.confirmedAt ? formatDateTime(order.confirmedAt) : "En attente"}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground block">Statut Stock:</span>
              <span className="font-medium">
                {order.status === "CONFIRMED" ? "Décrémenté (OUT)" : "Réservation DRAFT"}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground block">Entrepôt:</span>
              <span className="font-medium">
                {order.warehouseCode
                  ? `${order.warehouseCode}${order.warehouseName ? ` — ${order.warehouseName}` : ""}`
                  : order.warehouseName || "—"}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground block">Emplacement:</span>
              <span className="font-medium">
                {order.locationCode
                  ? `${order.locationCode}${order.locationName ? ` — ${order.locationName}` : ""}`
                  : order.locationName || "—"}
              </span>
            </div>
          </div>

          {/* Items Table */}
          <div className="border rounded-lg overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 text-xs text-muted-foreground border-b">
                <tr>
                  <th className="p-2 text-left">Article / Réf</th>
                  <th className="p-2 text-center w-24">Quantité</th>
                  <th className="p-2 text-right w-28">Prix unit. HT</th>
                  <th className="p-2 text-center w-20">TVA</th>
                  <th className="p-2 text-right w-28">Total HT</th>
                  <th className="p-2 text-right w-28">Total TTC</th>
                </tr>
              </thead>
              <tbody className="divide-y text-xs">
                {order.items.map((item) => (
                  <tr key={item.id} className="hover:bg-muted/20">
                    <td className="p-2">
                      <div className="font-medium">{item.articleDesignation}</div>
                      <div className="text-muted-foreground text-[11px]">
                        Réf: {item.articleReference}
                      </div>
                    </td>
                    <td className="p-2 text-center font-medium">
                      {formatQuantity(item.quantity, item.unitSymbol || undefined)}
                    </td>
                    <td className="p-2 text-right">
                      {formatCurrency(item.unitPrice)}
                    </td>
                    <td className="p-2 text-center text-muted-foreground">
                      {item.taxRate}%
                    </td>
                    <td className="p-2 text-right font-medium">
                      {formatCurrency(item.totalHt)}
                    </td>
                    <td className="p-2 text-right font-semibold text-primary">
                      {formatCurrency(item.totalTtc)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Totals & Notes */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              {order.notes && (
                <div className="bg-muted/20 border rounded-lg p-3 text-xs space-y-1">
                  <span className="font-medium text-muted-foreground">Notes:</span>
                  <p className="text-foreground">{order.notes}</p>
                </div>
              )}
            </div>

            <div className="bg-muted/40 rounded-lg p-3 space-y-1.5 text-xs sm:text-sm">
              <div className="flex justify-between text-muted-foreground">
                <span>Sous-total HT:</span>
                <span className="font-medium text-foreground">
                  {formatCurrency(order.subtotalHt)}
                </span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>TVA ({order.taxRate}%):</span>
                <span className="font-medium text-foreground">
                  {formatCurrency(order.taxAmount)}
                </span>
              </div>
              <div className="border-t pt-2 flex justify-between font-bold text-base">
                <span>Total TTC:</span>
                <span className="text-primary">{formatCurrency(order.totalTtc)}</span>
              </div>
            </div>
          </div>
        </DialogPanel>

        <DialogFooter className="mt-4 flex flex-wrap gap-2 justify-between">
          <div className="flex gap-2">
            {isAdmin && order.status === "DRAFT" && (
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={handleCancel}
                disabled={cancelMutation.isPending}
                className="gap-1.5"
              >
                <HugeiconsIcon icon={Cancel01Icon} className="size-4" />
                Annuler la commande
              </Button>
            )}
            {isAdmin && order.status === "CONFIRMED" && (
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={handleCancel}
                disabled={cancelMutation.isPending}
                className="gap-1.5"
              >
                <HugeiconsIcon icon={Cancel01Icon} className="size-4" />
                Annuler (Réintégrer stock)
              </Button>
            )}
          </div>

          <div className="flex gap-2">
            {isAdmin && order.status !== "CANCELLED" && order.items.length > 0 && (
              <Button
                type="button"
                variant="outline"
                onClick={handleCreateInvoice}
                disabled={createInvoiceMutation.isPending || customerInvoicesQuery.isLoading || existingCustomerInvoiceCount > 0}
              >
                {existingCustomerInvoiceCount > 0
                  ? "Facture déjà créée"
                  : createInvoiceMutation.isPending
                    ? "Création..."
                    : "Créer une facture client"}
              </Button>
            )}
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Fermer
            </Button>
            {isAdmin && order.status === "DRAFT" && (
              <Button
                type="button"
                className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5"
                onClick={handleConfirm}
                disabled={confirmMutation.isPending}
              >
                <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4" />
                {confirmMutation.isPending ? "Confirmation..." : "Confirmer la commande (Déduire stock)"}
              </Button>
            )}
          </div>
        </DialogFooter>
      </DialogPopup>
    </Dialog>
  );
}
