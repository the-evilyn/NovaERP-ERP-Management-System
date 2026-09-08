"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  InboxDownloadIcon,
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
import { useSupplierInvoices, useCreateSupplierInvoice } from "@/hooks/use-invoices";
import { useAuth } from "@/providers/auth-provider";
import { useCancelPurchaseOrder, useConfirmPurchaseOrder, useReceivePurchaseOrder } from "@/hooks/use-purchases";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatDateTime, formatQuantity } from "@/lib/formatters";
import type { PurchaseOrderResponse, PurchaseOrderStatus } from "@/types/models";

interface ViewPurchaseOrderDialogProps {
  order: PurchaseOrderResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ViewPurchaseOrderDialog({
  order,
  open,
  onOpenChange,
}: ViewPurchaseOrderDialogProps): React.ReactElement {
  const { user } = useAuth();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const confirmMutation = useConfirmPurchaseOrder();
  const receiveMutation = useReceivePurchaseOrder();
  const cancelMutation = useCancelPurchaseOrder();
  const createInvoiceMutation = useCreateSupplierInvoice();
  const supplierInvoicesQuery = useSupplierInvoices(0, 20, undefined, undefined, order?.id);

  const isAdmin = user?.role === "ADMIN";

  if (!order) return <></>;

  const existingSupplierInvoices = supplierInvoicesQuery.data?.content ?? [];
  const existingSupplierInvoiceCount = existingSupplierInvoices.length;

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

  const handleConfirm = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await confirmMutation.mutateAsync(order.id);
      setActionSuccess("Commande confirmée auprès du fournisseur.");
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Échec de la confirmation de la commande."));
    }
  };

  const handleReceive = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await receiveMutation.mutateAsync(order.id);
      setActionSuccess("Marchandise réceptionnée avec succès. Mouvements de stock ENTRÉE (IN) enregistrés.");
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Échec de la réception en stock."));
    }
  };

  const handleCancel = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await cancelMutation.mutateAsync(order.id);
      setActionSuccess("Commande d'achat annulée.");
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Impossible d'annuler la commande."));
    }
  };

  const handleCreateInvoice = async () => {
    setErrorMsg(null);
    setActionSuccess(null);
    try {
      await createInvoiceMutation.mutateAsync({
        supplierId: order.supplierId,
        purchaseOrderId: order.id,
        items: order.items.map((item) => ({
          articleId: item.articleId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          taxRate: item.taxRate,
        })),
        taxRate: order.taxRate,
        notes: order.notes ?? undefined,
      });
      setActionSuccess("Facture fournisseur créée à partir de cette commande.");
      setTimeout(() => onOpenChange(false), 1500);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Impossible de créer la facture fournisseur."));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-3xl max-h-[90vh] flex flex-col">
        <DialogHeader className="flex flex-row items-center justify-between pb-2 border-b">
          <div>
            <DialogTitle className="flex items-center gap-2 text-xl font-bold font-mono">
              <span>Bon d&apos;Achat : {order.orderNumber}</span>
              {getStatusBadge(order.status)}
            </DialogTitle>
            <p className="text-xs text-muted-foreground mt-0.5">
              Créé le {formatDateTime(order.createdAt)} {order.createdByName ? `par ${order.createdByName}` : ""}
            </p>
          </div>
        </DialogHeader>

        <DialogPanel className="flex-1 overflow-y-auto space-y-4 py-4 pr-1">
          {errorMsg && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
              <AlertDescription>{errorMsg}</AlertDescription>
            </Alert>
          )}

          {actionSuccess && (
            <Alert className="bg-emerald-50 text-emerald-800 border-emerald-200">
              <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4 text-emerald-600" />
              <AlertDescription>{actionSuccess}</AlertDescription>
            </Alert>
          )}

          {/* Supplier Info & Dates */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 p-3 bg-muted/30 rounded-lg border text-xs">
            <div>
              <span className="text-muted-foreground block">Fournisseur :</span>
              <span className="font-semibold text-foreground text-sm">{order.supplierName}</span>
            </div>
            <div>
              <span className="text-muted-foreground block">Confirmation fournisseur :</span>
              <span className="font-medium text-foreground">
                {order.confirmedAt ? formatDateTime(order.confirmedAt) : "Non confirmée"}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground block">Réception en stock :</span>
              <span className="font-medium text-foreground">
                {order.receivedAt ? formatDateTime(order.receivedAt) : "Non réceptionnée"}
              </span>
            </div>
          </div>

          {/* Order Items Table */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
              Articles commandés ({order.items.length})
            </h4>

            <div className="border rounded-md divide-y text-xs">
              <div className="grid grid-cols-12 gap-2 p-2.5 bg-muted/60 font-semibold text-muted-foreground">
                <div className="col-span-2">Réf.</div>
                <div className="col-span-4">Désignation</div>
                <div className="col-span-2 text-right">Quantité</div>
                <div className="col-span-2 text-right">Prix Unit. HT</div>
                <div className="col-span-2 text-right">Total HT</div>
              </div>

              {order.items.map((item) => (
                <div key={item.id} className="grid grid-cols-12 gap-2 p-2.5 items-center hover:bg-muted/20">
                  <div className="col-span-2 font-mono font-medium text-foreground">
                    {item.articleReference}
                  </div>
                  <div className="col-span-4 truncate" title={item.articleDesignation}>
                    {item.articleDesignation}
                  </div>
                  <div className="col-span-2 text-right font-medium">
                    {formatQuantity(item.quantity)} {item.unitSymbol || ""}
                  </div>
                  <div className="col-span-2 text-right">
                    {formatCurrency(item.unitPrice)}
                  </div>
                  <div className="col-span-2 text-right font-semibold text-foreground">
                    {formatCurrency(item.totalHt)}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Totals Summary */}
          <div className="flex flex-col items-end gap-1.5 pt-2 text-xs border-t">
            <div className="flex justify-between w-64 text-muted-foreground">
              <span>Sous-total HT :</span>
              <span className="font-medium text-foreground">{formatCurrency(order.subtotalHt)}</span>
            </div>
            <div className="flex justify-between w-64 text-muted-foreground">
              <span>TVA ({order.taxRate}%) :</span>
              <span className="font-medium text-foreground">{formatCurrency(order.taxAmount)}</span>
            </div>
            <div className="flex justify-between w-64 text-sm font-bold pt-1 border-t">
              <span>Total TTC :</span>
              <span className="text-primary font-mono text-base">{formatCurrency(order.totalTtc)}</span>
            </div>
          </div>

          {/* Notes */}
          {order.notes && (
            <div className="p-3 bg-muted/20 border rounded-md text-xs space-y-1">
              <span className="font-semibold text-muted-foreground">Instructions / Notes :</span>
              <p className="text-foreground whitespace-pre-wrap">{order.notes}</p>
            </div>
          )}

          {/* Stock impact note */}
          <div className="p-3 rounded-md bg-sky-50 border border-sky-100 text-sky-900 text-xs">
            <p className="font-semibold">Impact sur l&apos;inventaire :</p>
            <p className="mt-0.5">
              {order.status === "DRAFT" && "Statut brouillon : aucun impact de stock tant que la commande n'est pas réceptionnée."}
              {order.status === "CONFIRMED" && "Statut confirmé : en attente de livraison fournisseur. Cliquez sur Réceptionner pour incrémenter les stocks."}
              {order.status === "RECEIVED" && "Statut réceptionné : les articles ont été incrémentés en stock sous le mouvement IN."}
              {order.status === "CANCELLED" && "Statut annulé : aucun mouvement de stock actif pour cette commande."}
            </p>
          </div>
        </DialogPanel>

        <DialogFooter className="mt-4 flex flex-wrap gap-2 justify-between">
          <div className="flex gap-2">
            {isAdmin && (order.status === "DRAFT" || order.status === "CONFIRMED") && (
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
          </div>

          <div className="flex gap-2">
            {isAdmin && order.status !== "CANCELLED" && order.items.length > 0 && (
              <Button
                type="button"
                variant="outline"
                onClick={handleCreateInvoice}
                disabled={createInvoiceMutation.isPending || supplierInvoicesQuery.isLoading || existingSupplierInvoiceCount > 0}
              >
                {existingSupplierInvoiceCount > 0
                  ? "Facture déjà créée"
                  : createInvoiceMutation.isPending
                    ? "Création..."
                    : "Créer une facture fournisseur"}
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
                className="bg-amber-600 hover:bg-amber-700 text-white gap-1.5"
                onClick={handleConfirm}
                disabled={confirmMutation.isPending}
              >
                <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4" />
                {confirmMutation.isPending ? "Confirmation..." : "Confirmer la commande"}
              </Button>
            )}
            {isAdmin && order.status === "CONFIRMED" && (
              <Button
                type="button"
                className="bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5"
                onClick={handleReceive}
                disabled={receiveMutation.isPending}
              >
                <HugeiconsIcon icon={InboxDownloadIcon} className="size-4" />
                {receiveMutation.isPending ? "Réception..." : "Réceptionner (Entrée en stock)"}
              </Button>
            )}
          </div>
        </DialogFooter>
      </DialogPopup>
    </Dialog>
  );
}
