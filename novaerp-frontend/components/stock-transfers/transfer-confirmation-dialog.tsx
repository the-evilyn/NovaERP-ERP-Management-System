"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  Delete02Icon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogPanel,
  DialogPopup,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/ui/spinner";
import { getApiErrorMessage } from "@/lib/api-error";
import type { StockTransferResponse } from "@/types/models";

export type TransferActionType = "complete" | "cancel" | "delete";

interface TransferConfirmationDialogProps {
  transfer: StockTransferResponse | null;
  action: TransferActionType | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void>;
  onSuccess?: () => void;
}

export function TransferConfirmationDialog({
  transfer,
  action,
  open,
  onOpenChange,
  onConfirm,
  onSuccess,
}: TransferConfirmationDialogProps): React.ReactElement {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!transfer || !action) {
    return <></>;
  }

  const handleConfirm = async () => {
    setErrorMsg(null);
    setIsSubmitting(true);
    try {
      await onConfirm();
      setIsSubmitting(false);
      onOpenChange(false);
      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      setIsSubmitting(false);
      setErrorMsg(getApiErrorMessage(err));
    }
  };

  const getActionDetails = () => {
    switch (action) {
      case "complete":
        return {
          title: "Valider le transfert de stock",
          description: `Êtes-vous sûr de vouloir valider le transfert ${transfer.transferNumber} ? Cette action déplacera immédiatement le stock de la source (${transfer.sourceWarehouseCode}) vers la destination (${transfer.destinationWarehouseCode}) et enregistrera les mouvements d'inventaire. Cette opération est irréversible.`,
          confirmText: "Confirmer et transférer",
          confirmIcon: CheckmarkCircle02Icon,
          confirmVariant: "default" as const,
          confirmClassName: "bg-emerald-600 hover:bg-emerald-700 text-white",
        };
      case "cancel":
        return {
          title: "Annuler le transfert de stock",
          description: `Êtes-vous sûr de vouloir annuler le transfert ${transfer.transferNumber} ? Le transfert passera en statut Annulé. Aucun mouvement de stock ne sera effectué.`,
          confirmText: "Annuler le transfert",
          confirmIcon: Cancel01Icon,
          confirmVariant: "destructive" as const,
          confirmClassName: "",
        };
      case "delete":
        return {
          title: "Supprimer le transfert brouillon",
          description: `Êtes-vous sûr de vouloir supprimer définitivement le transfert ${transfer.transferNumber} ? Toutes les lignes d'articles associées seront supprimées. Cette action est irréversible.`,
          confirmText: "Supprimer définitivement",
          confirmIcon: Delete02Icon,
          confirmVariant: "destructive" as const,
          confirmClassName: "",
        };
    }
  };

  const details = getActionDetails();

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!isSubmitting) {
          setErrorMsg(null);
          onOpenChange(nextOpen);
        }
      }}
    >
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>{details.title}</DialogTitle>
          <DialogDescription className="mt-2">
            {details.description}
          </DialogDescription>
        </DialogHeader>

        <DialogPanel className="space-y-4">
          {errorMsg && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
              <AlertDescription className="text-xs">{errorMsg}</AlertDescription>
            </Alert>
          )}

          <div className="rounded-lg border bg-muted/30 p-3 text-xs space-y-1.5">
            <div className="flex justify-between">
              <span className="text-muted-foreground">N° Transfert :</span>
              <span className="font-semibold text-foreground">
                {transfer.transferNumber}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Source :</span>
              <span className="font-medium text-foreground">
                {transfer.sourceWarehouseCode}
                {transfer.sourceLocationCode ? ` / ${transfer.sourceLocationCode}` : " (Défaut)"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Destination :</span>
              <span className="font-medium text-foreground">
                {transfer.destinationWarehouseCode}
                {transfer.destinationLocationCode ? ` / ${transfer.destinationLocationCode}` : " (Défaut)"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Lignes d&apos;articles :</span>
              <span className="font-medium text-foreground">
                {transfer.items?.length ?? 0} article(s)
              </span>
            </div>
          </div>
        </DialogPanel>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={isSubmitting}
            onClick={() => onOpenChange(false)}
          >
            Fermer
          </Button>
          <Button
            type="button"
            variant={details.confirmVariant}
            className={details.confirmClassName}
            disabled={isSubmitting}
            onClick={handleConfirm}
          >
            {isSubmitting ? (
              <>
                <Spinner className="mr-2 size-4" />
                Traitement en cours...
              </>
            ) : (
              <>
                <HugeiconsIcon icon={details.confirmIcon} className="mr-2 size-4" />
                {details.confirmText}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogPopup>
    </Dialog>
  );
}
