"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon, CheckmarkCircle02Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogFooter,
  DialogHeader,
  DialogPanel,
  DialogPopup,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field, FieldLabel } from "@/components/ui/field";
import { Form } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateStockMovement } from "@/hooks/use-stock-movements";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ReorderRecommendationResponse } from "@/types/models";

import { formatCurrency } from "@/lib/formatters";

interface QuickReorderDialogProps {
  recommendation: ReorderRecommendationResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function QuickReorderDialog({
  recommendation,
  open,
  onOpenChange,
}: QuickReorderDialogProps): React.ReactElement {
  const [quantity, setQuantity] = useState("");
  const [reference, setReference] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loadedRecId, setLoadedRecId] = useState<number | null>(null);

  const createMovement = useCreateStockMovement();

  if (recommendation && loadedRecId !== recommendation.articleId) {
    setLoadedRecId(recommendation.articleId);
    setQuantity(String(recommendation.suggestedQuantity));
    setReference(`CMD-REAP-${recommendation.articleReference}`);
    setNote(
      `Réapprovisionnement suggéré (Aide à la décision). Fournisseur: ${recommendation.recommendedSupplierName}`,
    );
    setError(null);
    setSuccess(false);
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recommendation) return;
    const numQty = Number(quantity);
    if (!numQty || numQty <= 0) return;

    setError(null);
    try {
      await createMovement.mutateAsync({
        articleId: recommendation.articleId,
        type: "IN",
        quantity: numQty,
        reference: reference.trim(),
        note: note.trim(),
      });
      setSuccess(true);
      setTimeout(() => {
        setSuccess(false);
        onOpenChange(false);
      }, 1200);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Passer la commande de réapprovisionnement</DialogTitle>
        </DialogHeader>

        {recommendation && (
          <Form onSubmit={handleSubmit} id="quick-reorder-form">
            <DialogPanel className="flex flex-col gap-4">
              <div className="rounded-lg border bg-muted/40 p-3 text-sm">
                <div className="font-semibold text-foreground">
                  {recommendation.designation}
                </div>
                <div className="text-muted-foreground text-xs">
                  Réf: {recommendation.articleReference} &bull; Fournisseur recommandé:{" "}
                  <span className="font-medium text-foreground">
                    {recommendation.recommendedSupplierName}
                  </span>
                </div>
                <div className="mt-2 flex items-center justify-between text-xs">
                  <span>
                    Stock actuel:{" "}
                    <strong>
                      {recommendation.currentStock} {recommendation.unitName ?? ""}
                    </strong>
                  </span>
                  <span>
                    Budget estimé:{" "}
                    <strong>
                      {formatCurrency(
                        (Number(quantity) || recommendation.suggestedQuantity) *
                          recommendation.unitPrice
                      )}
                    </strong>
                  </span>
                </div>
              </div>

              {error && (
                <Alert variant="error">
                  <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {success && (
                <Alert variant="success">
                  <HugeiconsIcon icon={CheckmarkCircle02Icon} strokeWidth={2} />
                  <AlertDescription>
                    Entrée de stock enregistrée avec succès !
                  </AlertDescription>
                </Alert>
              )}

              <Field>
                <FieldLabel htmlFor="reorder-quantity">
                  Quantité à réceptionner
                </FieldLabel>
                <Input
                  id="reorder-quantity"
                  type="number"
                  step="any"
                  min="0.01"
                  required
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                />
              </Field>

              <Field>
                <FieldLabel htmlFor="reorder-ref">Référence de commande</FieldLabel>
                <Input
                  id="reorder-ref"
                  required
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                />
              </Field>

              <Field>
                <FieldLabel htmlFor="reorder-note">Note / Bon de livraison</FieldLabel>
                <Textarea
                  id="reorder-note"
                  rows={2}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </Field>
            </DialogPanel>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Annuler
              </Button>
              <Button
                type="submit"
                loading={createMovement.isPending}
                disabled={success}
              >
                Confirmer l&apos;entrée en stock
              </Button>
            </DialogFooter>
          </Form>
        )}
      </DialogPopup>
    </Dialog>
  );
}
