"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, AlertCircleIcon, Delete02Icon } from "@hugeicons/core-free-icons";
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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useArticles } from "@/hooks/use-articles";
import { useSuppliers } from "@/hooks/use-suppliers";
import { useCreatePurchaseOrder } from "@/hooks/use-purchases";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatQuantity } from "@/lib/formatters";
import type { PurchaseOrderItemRequest } from "@/types/models";

interface OrderLineState extends PurchaseOrderItemRequest {
  currentStock?: number;
  designation?: string;
  unitSymbol?: string | null;
}

interface CreatePurchaseOrderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreatePurchaseOrderDialog({
  open,
  onOpenChange,
}: CreatePurchaseOrderDialogProps): React.ReactElement {
  const [supplierId, setSupplierId] = useState<number | "">("");
  const [taxRate, setTaxRate] = useState<number>(20);
  const [notes, setNotes] = useState<string>("");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [lines, setLines] = useState<OrderLineState[]>([
    { articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 },
  ]);

  const { data: suppliersData, isLoading: loadingSuppliers } = useSuppliers(0, 100);
  const { data: articlesData, isLoading: loadingArticles } = useArticles(0, 300);
  const createOrderMutation = useCreatePurchaseOrder();

  const suppliers = suppliersData?.content ?? [];
  const articles = articlesData?.content ?? [];

  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      { articleId: 0, quantity: 1, unitPrice: 0, taxRate },
    ]);
  };

  const handleRemoveLine = (index: number) => {
    setLines((prev) => prev.filter((_, i) => i !== index));
  };

  const handleArticleSelect = (index: number, artId: number) => {
    const selected = articles.find((a) => a.id === artId);
    setLines((prev) =>
      prev.map((line, i) => {
        if (i !== index) return line;
        return {
          ...line,
          articleId: artId,
          unitPrice: selected?.purchasePriceHt ?? 0,
          currentStock: selected?.stockQuantity ?? 0,
          designation: selected?.designation ?? "",
          unitSymbol: selected?.unitName ?? null,
        };
      }),
    );
  };

  const handleLineChange = (
    index: number,
    field: keyof PurchaseOrderItemRequest,
    value: number,
  ) => {
    setLines((prev) =>
      prev.map((line, i) => {
        if (i !== index) return line;
        return { ...line, [field]: value };
      }),
    );
  };

  // Calculations
  const subtotalHt = lines.reduce(
    (acc, l) => acc + (l.quantity || 0) * (l.unitPrice || 0),
    0,
  );
  const totalTax = lines.reduce((acc, l) => {
    const lineHt = (l.quantity || 0) * (l.unitPrice || 0);
    return acc + lineHt * ((l.taxRate ?? taxRate) / 100);
  }, 0);
  const totalTtc = subtotalHt + totalTax;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!supplierId) {
      setErrorMsg("Veuillez sélectionner un fournisseur.");
      return;
    }

    if (lines.length === 0 || lines.some((l) => !l.articleId || l.quantity <= 0)) {
      setErrorMsg("Veuillez renseigner correctement les lignes d'articles et quantités.");
      return;
    }

    try {
      await createOrderMutation.mutateAsync({
        supplierId: Number(supplierId),
        taxRate,
        notes: notes.trim() || undefined,
        items: lines.map((l) => ({
          articleId: l.articleId,
          quantity: l.quantity,
          unitPrice: l.unitPrice,
          taxRate: l.taxRate,
        })),
      });

      // Reset and close
      setSupplierId("");
      setNotes("");
      setLines([{ articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 }]);
      onOpenChange(false);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err, "Erreur lors de la création de la commande d'achat."));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-3xl max-h-[90vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>Nouvelle Commande d&apos;Achat (Approvisionnement)</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
          <DialogPanel className="flex-1 overflow-y-auto space-y-4 pr-1">
            {errorMsg && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                <AlertDescription>{errorMsg}</AlertDescription>
              </Alert>
            )}

            {/* Supplier & Global Tax */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="po-supplier-select">Fournisseur *</FieldLabel>
                <select
                  id="po-supplier-select"
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value ? Number(e.target.value) : "")}
                  required
                  aria-label="Sélectionner un fournisseur"
                  className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  <option value="">-- Choisir un fournisseur --</option>
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} {s.phone ? `(${s.phone})` : ""}
                    </option>
                  ))}
                </select>
                {loadingSuppliers && (
                  <span className="text-xs text-muted-foreground">Chargement des fournisseurs...</span>
                )}
              </Field>

              <Field>
                <FieldLabel htmlFor="po-tax-rate">Taux TVA global (%)</FieldLabel>
                <Input
                  id="po-tax-rate"
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  value={taxRate}
                  onChange={(e) => setTaxRate(Number(e.target.value) || 0)}
                />
              </Field>
            </div>

            {/* Items Table */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">Articles à commander</span>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleAddLine}
                  className="gap-1 text-xs"
                >
                  <HugeiconsIcon icon={Add01Icon} className="size-3.5" />
                  Ajouter un article
                </Button>
              </div>

              <div className="border rounded-md divide-y text-sm">
                <div className="grid grid-cols-12 gap-2 p-2.5 bg-muted/50 font-medium text-xs text-muted-foreground">
                  <div className="col-span-5">Article & Réf.</div>
                  <div className="col-span-2 text-right">Quantité</div>
                  <div className="col-span-2 text-right">Prix Achat HT</div>
                  <div className="col-span-2 text-right">Total HT</div>
                  <div className="col-span-1 text-center">Action</div>
                </div>

                {lines.map((line, idx) => {
                  const lineTotalHt = (line.quantity || 0) * (line.unitPrice || 0);

                  return (
                    <div
                      key={idx}
                      className="grid grid-cols-12 gap-2 p-2.5 items-center hover:bg-muted/20"
                    >
                      <div className="col-span-5 space-y-1">
                        <select
                          value={line.articleId || ""}
                          onChange={(e) => handleArticleSelect(idx, Number(e.target.value))}
                          aria-label={`Sélectionner un article pour la ligne ${idx + 1}`}
                          className="flex h-8 w-full rounded-md border border-input bg-background px-2 text-xs shadow-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                        >
                          <option value="">-- Choisir un article --</option>
                          {articles.map((art) => (
                            <option key={art.id} value={art.id}>
                              {art.reference} — {art.designation} (Stock: {formatQuantity(art.stockQuantity ?? 0)})
                            </option>
                          ))}
                        </select>
                        {loadingArticles && (
                          <span className="text-[10px] text-muted-foreground">Chargement articles...</span>
                        )}
                      </div>

                      <div className="col-span-2">
                        <Input
                          type="number"
                          step="1"
                          min="0.001"
                          value={line.quantity}
                          aria-label={`Quantité pour la ligne ${idx + 1}`}
                          onChange={(e) =>
                            handleLineChange(idx, "quantity", Number(e.target.value) || 0)
                          }
                          className="h-8 text-xs text-right"
                        />
                      </div>

                      <div className="col-span-2">
                        <Input
                          type="number"
                          step="0.01"
                          min="0"
                          value={line.unitPrice}
                          aria-label={`Prix d'achat pour la ligne ${idx + 1}`}
                          onChange={(e) =>
                            handleLineChange(idx, "unitPrice", Number(e.target.value) || 0)
                          }
                          className="h-8 text-xs text-right"
                        />
                      </div>

                      <div className="col-span-2 text-right font-medium text-xs">
                        {formatCurrency(lineTotalHt)}
                      </div>

                      <div className="col-span-1 text-center">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-sm"
                          disabled={lines.length === 1}
                          onClick={() => handleRemoveLine(idx)}
                          className="text-destructive hover:text-destructive size-7"
                        >
                          <HugeiconsIcon icon={Delete02Icon} className="size-3.5" />
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Order Totals Summary */}
            <div className="flex flex-col items-end gap-1.5 pt-2 border-t text-sm">
              <div className="flex justify-between w-64 text-muted-foreground text-xs">
                <span>Sous-total HT :</span>
                <span className="font-medium text-foreground">{formatCurrency(subtotalHt)}</span>
              </div>
              <div className="flex justify-between w-64 text-muted-foreground text-xs">
                <span>TVA estimée :</span>
                <span className="font-medium text-foreground">{formatCurrency(totalTax)}</span>
              </div>
              <div className="flex justify-between w-64 text-base font-bold pt-1 border-t">
                <span>Total TTC :</span>
                <span className="text-primary">{formatCurrency(totalTtc)}</span>
              </div>
            </div>

            {/* Notes */}
            <Field>
              <FieldLabel htmlFor="po-notes">Notes / Instructions fournisseur</FieldLabel>
              <Textarea
                id="po-notes"
                placeholder="Délai souhaité, conditions de livraison..."
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </Field>
          </DialogPanel>

          <DialogFooter className="mt-4 pt-3 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              disabled={createOrderMutation.isPending}
            >
              {createOrderMutation.isPending ? "Création..." : "Créer le bon d'achat (Brouillon)"}
            </Button>
          </DialogFooter>
        </form>
      </DialogPopup>
    </Dialog>
  );
}
