"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  ShoppingCart01Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
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
import { useCreatePurchaseOrder } from "@/hooks/use-purchases";
import { useSuppliers } from "@/hooks/use-suppliers";
import { useWarehouseLocations, useWarehouses } from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import type { ReorderRecommendationResponse } from "@/types/models";

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
  const [supplierId, setSupplierId] = useState<number | "">("");
  const [quantity, setQuantity] = useState("");
  const [unitPrice, setUnitPrice] = useState("");
  const [taxRate, setTaxRate] = useState<number>(20);
  const [warehouseId, setWarehouseId] = useState<number | "">("");
  const [locationId, setLocationId] = useState<number | "">("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [createdOrderNumber, setCreatedOrderNumber] = useState<string | null>(null);
  const [loadedRecId, setLoadedRecId] = useState<number | null>(null);

  const { data: suppliersData, isLoading: loadingSuppliers } = useSuppliers(0, 100);
  const { data: warehousesData, isLoading: loadingWarehouses } = useWarehouses(0, 100, true);
  const { data: locationsData } = useWarehouseLocations(
    typeof warehouseId === "number" ? warehouseId : 0,
    0,
    100,
    true,
  );
  const createOrderMutation = useCreatePurchaseOrder();

  const suppliers = suppliersData?.content ?? [];
  const warehouses = warehousesData?.content ?? [];
  const locations = locationsData?.content ?? [];

  if (recommendation && loadedRecId !== recommendation.articleId) {
    setLoadedRecId(recommendation.articleId);
    setSupplierId(recommendation.recommendedSupplierId ?? "");
    setQuantity(String(recommendation.suggestedQuantity ?? 1));
    setUnitPrice(String(recommendation.unitPrice ?? 0));
    setWarehouseId("");
    setLocationId("");
    setTaxRate(20);
    setNote(
      `Réapprovisionnement suggéré (Aide à la décision) - Réf: ${recommendation.articleReference} - Fournisseur: ${recommendation.recommendedSupplierName}`,
    );
    setError(null);
    setCreatedOrderNumber(null);
  }

  const numQty = Number(quantity) || 0;
  const numPrice = Number(unitPrice) || 0;
  const subtotalHt = numQty * numPrice;
  const totalTax = subtotalHt * (taxRate / 100);
  const totalTtc = subtotalHt + totalTax;

  const handleWarehouseChange = (value: string) => {
    const nextWhId = value ? Number(value) : "";
    setWarehouseId(nextWhId);
    setLocationId("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recommendation) return;

    if (!supplierId) {
      setError("Veuillez sélectionner un fournisseur.");
      return;
    }

    if (numQty <= 0) {
      setError("La quantité commandée doit être strictement positive.");
      return;
    }

    if (numPrice < 0) {
      setError("Le prix unitaire ne peut pas être négatif.");
      return;
    }

    if (!warehouseId && locationId) {
      setError("Un entrepôt doit être sélectionné si un emplacement est spécifié.");
      return;
    }

    setError(null);
    try {
      const result = await createOrderMutation.mutateAsync({
        supplierId: Number(supplierId),
        taxRate,
        notes: note.trim() || undefined,
        warehouseId: warehouseId !== "" ? Number(warehouseId) : undefined,
        locationId: locationId !== "" ? Number(locationId) : undefined,
        items: [
          {
            articleId: recommendation.articleId,
            quantity: numQty,
            unitPrice: numPrice,
            taxRate,
          },
        ],
      });

      setCreatedOrderNumber(result.orderNumber);
      setTimeout(() => {
        setCreatedOrderNumber(null);
        onOpenChange(false);
      }, 2200);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer la commande d'achat."));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-xl max-h-[90vh] flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <HugeiconsIcon icon={ShoppingCart01Icon} className="size-5 text-primary" />
            <span>Passer commande de réapprovisionnement</span>
          </DialogTitle>
        </DialogHeader>

        {recommendation && (
          <Form onSubmit={handleSubmit} id="quick-reorder-form" className="flex flex-col flex-1 overflow-hidden">
            <DialogPanel className="flex-1 overflow-y-auto space-y-4 py-2 pr-1">
              {/* Summary Card */}
              <div className="rounded-lg border bg-muted/40 p-3 text-sm">
                <div className="font-semibold text-foreground">
                  {recommendation.designation}
                </div>
                <div className="text-muted-foreground text-xs mt-0.5">
                  Réf: {recommendation.articleReference} &bull; Catégorie:{" "}
                  {recommendation.categoryName ?? "—"}
                </div>
                <div className="mt-2.5 grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-border/50 text-xs">
                  <div>
                    <span className="text-muted-foreground block">Stock actuel :</span>
                    <span className="font-semibold text-foreground">
                      {recommendation.currentStock} {recommendation.unitName ?? ""}
                    </span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">Seuil min :</span>
                    <span className="font-medium text-foreground">
                      {recommendation.minStockQuantity} {recommendation.unitName ?? ""}
                    </span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">Total HT :</span>
                    <span className="font-semibold text-primary">
                      {formatCurrency(subtotalHt)}
                    </span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">Total TTC ({taxRate}%) :</span>
                    <span className="font-bold text-foreground">
                      {formatCurrency(totalTtc)}
                    </span>
                  </div>
                </div>
              </div>

              {error && (
                <Alert variant="error">
                  <HugeiconsIcon icon={AlertCircleIcon} strokeWidth={2} />
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {createdOrderNumber && (
                <Alert variant="success">
                  <HugeiconsIcon icon={CheckmarkCircle02Icon} strokeWidth={2} />
                  <AlertDescription className="flex flex-col gap-1">
                    <span className="font-semibold">
                      Bon de commande d&apos;achat {createdOrderNumber} créé en statut BROUILLON (DRAFT) !
                    </span>
                    <span className="text-xs text-muted-foreground">
                      Aucun mouvement de stock n&apos;a été effectué. Le stock sera incrémenté lors de la réception ultérieure de la marchandise.
                    </span>
                  </AlertDescription>
                </Alert>
              )}

              {/* Form inputs */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Field>
                  <FieldLabel htmlFor="reorder-supplier">Fournisseur *</FieldLabel>
                  <select
                    id="reorder-supplier"
                    value={supplierId}
                    onChange={(e) => setSupplierId(e.target.value ? Number(e.target.value) : "")}
                    required
                    aria-label="Sélectionner un fournisseur"
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    <option value="">-- Choisir un fournisseur --</option>
                    {suppliers.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} {s.id === recommendation.recommendedSupplierId ? "★ (Recommandé)" : ""}
                      </option>
                    ))}
                  </select>
                  {loadingSuppliers && (
                    <span className="text-xs text-muted-foreground">Chargement des fournisseurs...</span>
                  )}
                </Field>

                <Field>
                  <FieldLabel htmlFor="reorder-tax-rate">Taux TVA (%)</FieldLabel>
                  <Input
                    id="reorder-tax-rate"
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    value={taxRate}
                    onChange={(e) => setTaxRate(Number(e.target.value) || 0)}
                  />
                </Field>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Field>
                  <FieldLabel htmlFor="reorder-quantity">
                    Quantité à commander *
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
                  <FieldLabel htmlFor="reorder-unit-price">
                    Prix unitaire d&apos;achat HT *
                  </FieldLabel>
                  <Input
                    id="reorder-unit-price"
                    type="number"
                    step="any"
                    min="0"
                    required
                    value={unitPrice}
                    onChange={(e) => setUnitPrice(e.target.value)}
                  />
                </Field>
              </div>

              {/* Warehouse & Location */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Field>
                  <FieldLabel htmlFor="reorder-warehouse">Entrepôt de destination (Optionnel)</FieldLabel>
                  <select
                    id="reorder-warehouse"
                    value={warehouseId}
                    onChange={(e) => handleWarehouseChange(e.target.value)}
                    disabled={loadingWarehouses}
                    aria-label="Sélectionner un entrepôt de destination"
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    <option value="">-- Principal / Hérité --</option>
                    {warehouses.map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.code} — {w.name}
                      </option>
                    ))}
                  </select>
                </Field>

                <Field>
                  <FieldLabel htmlFor="reorder-location">Emplacement (Optionnel)</FieldLabel>
                  <select
                    id="reorder-location"
                    value={locationId}
                    onChange={(e) => setLocationId(e.target.value ? Number(e.target.value) : "")}
                    disabled={!warehouseId || locations.length === 0}
                    aria-label="Sélectionner un emplacement"
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:opacity-50"
                  >
                    <option value="">-- Par défaut (LOC-GEN) --</option>
                    {locations.map((loc) => (
                      <option key={loc.id} value={loc.id}>
                        {loc.code} — {loc.name}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>

              <Field>
                <FieldLabel htmlFor="reorder-note">Note / Remarque</FieldLabel>
                <Textarea
                  id="reorder-note"
                  rows={2}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </Field>

              {/* Workflow Info Note */}
              <div className="rounded-md border border-sky-500/20 bg-sky-50/50 p-2.5 text-xs text-sky-800 dark:bg-sky-950/20 dark:text-sky-300">
                <p className="font-semibold">Traçabilité & Flux d&apos;approvisionnement :</p>
                <p className="mt-0.5">
                  Cette action crée un <strong>Bon de Commande d&apos;Achat (DRAFT)</strong>. Aucun mouvement de stock n&apos;est enregistré immédiatement. Vous pourrez relire ou modifier la commande avant confirmation et réception physique en stock.
                </p>
              </div>
            </DialogPanel>

            <DialogFooter className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t pt-3">
              <div>
                {createdOrderNumber && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    render={<Link href="/purchases" />}
                  >
                    Voir les commandes d&apos;achat
                  </Button>
                )}
              </div>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onOpenChange(false)}
                >
                  Annuler
                </Button>
                <Button
                  type="submit"
                  loading={createOrderMutation.isPending}
                  disabled={Boolean(createdOrderNumber)}
                >
                  Créer le bon de commande (DRAFT)
                </Button>
              </div>
            </DialogFooter>
          </Form>
        )}
      </DialogPopup>
    </Dialog>
  );
}
