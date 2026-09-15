"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  ArrowLeft01Icon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  Delete02Icon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type React from "react";
import { useState } from "react";
import { StockTransferStatusBadge } from "@/components/stock-transfers/stock-transfer-status-badge";
import {
  TransferConfirmationDialog,
  type TransferActionType,
} from "@/components/stock-transfers/transfer-confirmation-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import {
  useCancelStockTransfer,
  useCompleteStockTransfer,
  useDeleteStockTransfer,
  useStockTransfer,
} from "@/hooks/use-stock-transfers";
import { formatDateTime, formatQuantity } from "@/lib/formatters";

interface StockTransferDetailsProps {
  id: number;
}

export function StockTransferDetails({
  id,
}: StockTransferDetailsProps): React.ReactElement {
  const router = useRouter();

  const { data: transfer, isLoading, isError } = useStockTransfer(id);

  const [dialogAction, setDialogAction] = useState<TransferActionType | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const completeMutation = useCompleteStockTransfer();
  const cancelMutation = useCancelStockTransfer();
  const deleteMutation = useDeleteStockTransfer();

  const openActionDialog = (action: TransferActionType) => {
    setDialogAction(action);
    setDialogOpen(true);
  };

  const handleConfirmAction = async () => {
    if (!transfer || !dialogAction) return;

    if (dialogAction === "complete") {
      await completeMutation.mutateAsync(transfer.id);
    } else if (dialogAction === "cancel") {
      await cancelMutation.mutateAsync(transfer.id);
    } else if (dialogAction === "delete") {
      await deleteMutation.mutateAsync(transfer.id);
      router.push("/stock/transfers");
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="flex items-center gap-2 text-muted-foreground text-sm">
          <Spinner className="size-5" />
          Chargement des détails du transfert...
        </div>
      </div>
    );
  }

  if (isError || !transfer) {
    return (
      <div className="space-y-4">
        <Button
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => router.push("/stock/transfers")}
        >
          <HugeiconsIcon icon={ArrowLeft01Icon} className="size-4" />
          Retour aux transferts
        </Button>
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
          <AlertDescription>
            Impossible de charger le transfert de stock demandé. Il se peut qu&apos;il n&apos;existe pas ou ait été supprimé.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const isDraft = transfer.status === "DRAFT";
  const isCompleted = transfer.status === "COMPLETED";
  const isCancelled = transfer.status === "CANCELLED";

  return (
    <div className="space-y-6">
      {/* Action Dialog */}
      <TransferConfirmationDialog
        transfer={transfer}
        action={dialogAction}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onConfirm={handleConfirmAction}
      />

      {/* Header and Actions */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2.5">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => router.push("/stock/transfers")}
          >
            <HugeiconsIcon icon={ArrowLeft01Icon} className="size-4" />
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-xl tracking-tight">
                {transfer.transferNumber}
              </h1>
              <StockTransferStatusBadge status={transfer.status} />
            </div>
            <p className="text-muted-foreground text-xs">
              Créé le {formatDateTime(transfer.createdAt)}
              {transfer.createdByName ? ` par ${transfer.createdByName}` : ""}
            </p>
          </div>
        </div>

        {/* Actions Bar for DRAFT */}
        <div className="flex flex-wrap items-center gap-2">
          {isDraft && (
            <>
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5"
                render={<Link href={`/stock/transfers/${transfer.id}/edit`} />}
              >
                <HugeiconsIcon icon={PencilEdit01Icon} className="size-3.5" />
                Modifier
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-muted-foreground hover:text-foreground"
                onClick={() => openActionDialog("cancel")}
              >
                <HugeiconsIcon icon={Cancel01Icon} className="size-3.5" />
                Annuler
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-destructive hover:text-destructive hover:bg-destructive/10"
                onClick={() => openActionDialog("delete")}
              >
                <HugeiconsIcon icon={Delete02Icon} className="size-3.5" />
                Supprimer
              </Button>

              <Button
                size="sm"
                className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => openActionDialog("complete")}
              >
                <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3.5" />
                Valider le transfert
              </Button>
            </>
          )}

          {!isDraft && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => router.push("/stock/transfers")}
            >
              Retour à la liste
            </Button>
          )}
        </div>
      </div>

      {/* Status Notice Banner */}
      {isCompleted && (
        <Alert variant="default" className="border-emerald-200 bg-emerald-50/50 dark:border-emerald-900 dark:bg-emerald-950/20 text-emerald-900 dark:text-emerald-200 text-xs">
          <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4 shrink-0 text-emerald-600" />
          <AlertDescription>
            Ce transfert a été validé et exécuté avec succès le{" "}
            <span className="font-semibold">{formatDateTime(transfer.completedAt)}</span>.
            Les stocks source ont été décrémentés et les stocks destination incrémentés.
          </AlertDescription>
        </Alert>
      )}

      {isCancelled && (
        <Alert variant="default" className="border-destructive/20 bg-destructive/5 text-destructive text-xs">
          <HugeiconsIcon icon={Cancel01Icon} className="size-4 shrink-0" />
          <AlertDescription>
            Ce transfert a été annulé le{" "}
            <span className="font-semibold">{formatDateTime(transfer.cancelledAt)}</span>.
            Aucun mouvement de stock n&apos;a été enregistré.
          </AlertDescription>
        </Alert>
      )}

      {/* Info Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Source Warehouse Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
              <span className="flex size-2 rounded-full bg-amber-500" />
              Origine / Source
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-xs">
            <div className="text-sm font-semibold text-foreground">
              {transfer.sourceWarehouseCode}
            </div>
            <div className="text-muted-foreground">
              {transfer.sourceWarehouseName}
            </div>
            <div className="pt-2 border-t mt-2">
              <span className="text-muted-foreground">Emplacement : </span>
              <span className="font-medium text-foreground">
                {transfer.sourceLocationCode
                  ? `${transfer.sourceLocationCode} (${transfer.sourceLocationName ?? ""})`
                  : "Emplacement par défaut (LOC-GEN)"}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Destination Warehouse Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
              <span className="flex size-2 rounded-full bg-emerald-500" />
              Destination
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-xs">
            <div className="text-sm font-semibold text-foreground">
              {transfer.destinationWarehouseCode}
            </div>
            <div className="text-muted-foreground">
              {transfer.destinationWarehouseName}
            </div>
            <div className="pt-2 border-t mt-2">
              <span className="text-muted-foreground">Emplacement : </span>
              <span className="font-medium text-foreground">
                {transfer.destinationLocationCode
                  ? `${transfer.destinationLocationCode} (${transfer.destinationLocationName ?? ""})`
                  : "Emplacement par défaut (LOC-GEN)"}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Audit & Timing Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Détails & Suivi
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Créé par :</span>
              <span className="font-medium text-foreground">
                {transfer.createdByName ?? "—"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Date de création :</span>
              <span className="text-foreground">
                {formatDateTime(transfer.createdAt)}
              </span>
            </div>
            {transfer.completedAt && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Date de validation :</span>
                <span className="text-foreground">
                  {formatDateTime(transfer.completedAt)}
                </span>
              </div>
            )}
            {transfer.cancelledAt && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Date d&apos;annulation :</span>
                <span className="text-foreground">
                  {formatDateTime(transfer.cancelledAt)}
                </span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total lignes :</span>
              <span className="font-semibold text-foreground">
                {transfer.items?.length ?? 0} article(s)
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Notes Card if present */}
      {transfer.notes && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Notes & Observations
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-xs text-foreground whitespace-pre-wrap">
              {transfer.notes}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Items Table Card */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-semibold flex items-center justify-between">
            <span>Articles inclus dans le transfert</span>
            <span className="text-xs font-normal text-muted-foreground">
              {transfer.items?.length ?? 0} article(s)
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="border rounded-lg overflow-x-auto">
            <table className="w-full text-xs">
              <thead className="bg-muted/50 text-muted-foreground border-b">
                <tr>
                  <th className="p-2.5 text-left w-12">#</th>
                  <th className="p-2.5 text-left">Référence</th>
                  <th className="p-2.5 text-left">Désignation</th>
                  <th className="p-2.5 text-right">Quantité transférée</th>
                  <th className="p-2.5 text-left w-28">Unité</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {transfer.items?.map((item, index) => (
                  <tr key={item.id ?? index} className="hover:bg-muted/10">
                    <td className="p-2.5 text-muted-foreground font-mono">
                      {index + 1}
                    </td>
                    <td className="p-2.5 font-semibold text-foreground">
                      {item.articleReference}
                    </td>
                    <td className="p-2.5 text-foreground">
                      {item.articleDesignation}
                    </td>
                    <td className="p-2.5 text-right font-medium text-foreground">
                      {formatQuantity(item.quantity, item.unitSymbol)}
                    </td>
                    <td className="p-2.5 text-muted-foreground">
                      {item.unitSymbol ?? "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
