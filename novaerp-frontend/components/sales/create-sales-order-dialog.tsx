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
import { useClients } from "@/hooks/use-clients";
import { useCreateSaleOrder } from "@/hooks/use-sales";
import { useWarehouseLocations, useWarehouses } from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatQuantity } from "@/lib/formatters";
import type { SaleOrderItemRequest } from "@/types/models";

interface OrderLineState extends SaleOrderItemRequest {
  availableStock?: number;
  designation?: string;
  unitSymbol?: string | null;
}

interface CreateSalesOrderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateSalesOrderDialog({
  open,
  onOpenChange,
}: CreateSalesOrderDialogProps): React.ReactElement {
  const [clientId, setClientId] = useState<number | "">("");
  const [warehouseId, setWarehouseId] = useState<number | "">("");
  const [locationId, setLocationId] = useState<number | "">("");
  const [taxRate, setTaxRate] = useState<number>(20);
  const [notes, setNotes] = useState<string>("");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [lines, setLines] = useState<OrderLineState[]>([
    { articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 },
  ]);

  const { data: clientsData, isLoading: loadingClients } = useClients(0, 100);
  const { data: articlesData, isLoading: loadingArticles } = useArticles(0, 300);
  const { data: warehousesData, isLoading: loadingWarehouses } = useWarehouses(0, 100, true);
  const { data: locationsData, isLoading: loadingLocations } = useWarehouseLocations(
    typeof warehouseId === "number" ? warehouseId : 0,
    0,
    100,
    true,
  );
  const createOrderMutation = useCreateSaleOrder();

  const clients = clientsData?.content ?? [];
  const articles = articlesData?.content ?? [];
  const warehouses = warehousesData?.content ?? [];
  const locations = locationsData?.content ?? [];

  const handleWarehouseChange = (value: string) => {
    const nextWhId = value ? Number(value) : "";
    setWarehouseId(nextWhId);
    setLocationId("");
  };

  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      { articleId: 0, quantity: 1, unitPrice: 0, taxRate },
    ]);
  };

  const handleRemoveLine = (index: number) => {
    if (lines.length <= 1) return;
    setLines((prev) => prev.filter((_, i) => i !== index));
  };

  const handleArticleChange = (index: number, articleId: number) => {
    const selected = articles.find((a) => a.id === articleId);
    setLines((prev) => {
      const next = [...prev];
      next[index] = {
        ...next[index],
        articleId,
        unitPrice: selected?.salePriceHt ?? 0,
        availableStock: selected?.stockQuantity ?? 0,
        designation: selected?.designation ?? "",
        unitSymbol: selected?.unitName ?? null,
      };
      return next;
    });
  };

  const handleQuantityChange = (index: number, qty: number) => {
    setLines((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], quantity: qty };
      return next;
    });
  };

  const handleUnitPriceChange = (index: number, price: number) => {
    setLines((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], unitPrice: price };
      return next;
    });
  };

  // Calculate live totals
  const subtotalHt = lines.reduce((acc, line) => {
    if (line.articleId > 0 && line.quantity > 0) {
      return acc + line.quantity * line.unitPrice;
    }
    return acc;
  }, 0);

  const taxAmount = lines.reduce((acc, line) => {
    if (line.articleId > 0 && line.quantity > 0) {
      const lineTax = (line.quantity * line.unitPrice * (line.taxRate ?? taxRate)) / 100;
      return acc + lineTax;
    }
    return acc;
  }, 0);

  const totalTtc = subtotalHt + taxAmount;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!clientId) {
      setErrorMsg("Veuillez sélectionner un client.");
      return;
    }

    if (!warehouseId && locationId) {
      setErrorMsg("Un entrepôt doit être sélectionné si un emplacement est spécifié.");
      return;
    }

    const validLines = lines.filter((l) => l.articleId > 0 && l.quantity > 0);
    if (validLines.length === 0) {
      setErrorMsg("Veuillez ajouter au moins un article valide.");
      return;
    }

    try {
      await createOrderMutation.mutateAsync({
        clientId: Number(clientId),
        taxRate,
        notes: notes.trim() || undefined,
        warehouseId: warehouseId !== "" ? Number(warehouseId) : undefined,
        locationId: locationId !== "" ? Number(locationId) : undefined,
        items: validLines.map((l) => ({
          articleId: l.articleId,
          quantity: l.quantity,
          unitPrice: l.unitPrice,
          taxRate: l.taxRate ?? taxRate,
        })),
      });

      // Reset and close
      setClientId("");
      setWarehouseId("");
      setLocationId("");
      setNotes("");
      setLines([{ articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 }]);
      onOpenChange(false);
    } catch (err) {
      setErrorMsg(getApiErrorMessage(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Nouvelle Commande de Vente</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <DialogPanel className="space-y-4">
            {errorMsg && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                <AlertDescription>{errorMsg}</AlertDescription>
              </Alert>
            )}

            {/* Header Form: Client & TVA */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2">
                <Field>
                  <FieldLabel>Client *</FieldLabel>
                  <select
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    value={clientId}
                    onChange={(e) =>
                      setClientId(e.target.value ? Number(e.target.value) : "")
                    }
                    disabled={loadingClients}
                    required
                  >
                    <option value="">
                      {loadingClients ? "Chargement des clients..." : "-- Sélectionner un client --"}
                    </option>
                    {clients.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name} {c.city ? `(${c.city})` : ""}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>

              <div>
                <Field>
                  <FieldLabel>Taux TVA standard (%)</FieldLabel>
                  <Input
                    type="number"
                    min="0"
                    max="100"
                    step="0.5"
                    value={taxRate}
                    onChange={(e) => setTaxRate(Number(e.target.value))}
                  />
                </Field>
              </div>
            </div>

            {/* Warehouse & Location Selectors */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Field>
                  <FieldLabel>Entrepôt (Optionnel)</FieldLabel>
                  <select
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    value={warehouseId}
                    onChange={(e) => handleWarehouseChange(e.target.value)}
                    disabled={loadingWarehouses}
                  >
                    <option value="">
                      {loadingWarehouses
                        ? "Chargement des entrepôts..."
                        : "-- Aucun entrepôt (Hérité / Principal) --"}
                    </option>
                    {warehouses.map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.code} — {w.name}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>

              <div>
                <Field>
                  <FieldLabel>Emplacement (Optionnel)</FieldLabel>
                  <select
                    className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                    value={locationId}
                    onChange={(e) =>
                      setLocationId(e.target.value ? Number(e.target.value) : "")
                    }
                    disabled={warehouseId === "" || loadingLocations}
                  >
                    <option value="">
                      {warehouseId === ""
                        ? "-- Sélectionner d'abord un entrepôt --"
                        : loadingLocations
                          ? "Chargement des emplacements..."
                          : "-- Emplacement par défaut --"}
                    </option>
                    {locations.map((loc) => (
                      <option key={loc.id} value={loc.id}>
                        {loc.code} — {loc.name} {loc.isDefault ? "(Défaut)" : ""}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
            </div>

            {/* Items Table */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-medium">Lignes de la commande</h4>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleAddLine}
                  className="h-8 gap-1 text-xs"
                >
                  <HugeiconsIcon icon={Add01Icon} className="size-3.5" />
                  Ajouter un article
                </Button>
              </div>

              <div className="border rounded-lg overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-muted/50 text-xs text-muted-foreground border-b">
                    <tr>
                      <th className="p-2 text-left min-w-[240px]">Article</th>
                      <th className="p-2 text-left w-24">Quantité</th>
                      <th className="p-2 text-left w-28">Prix unit. HT</th>
                      <th className="p-2 text-right w-24">Total HT</th>
                      <th className="p-2 text-center w-12"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {lines.map((line, idx) => {
                      const lineTotalHt = line.quantity * line.unitPrice;
                      const hasStockWarning =
                        line.availableStock !== undefined &&
                        line.availableStock < line.quantity;

                      return (
                        <tr key={idx} className="hover:bg-muted/20">
                          <td className="p-2">
                            <select
                              className="flex h-8 w-full rounded-md border border-input bg-background px-2 py-1 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                              value={line.articleId || ""}
                              onChange={(e) =>
                                handleArticleChange(idx, Number(e.target.value))
                              }
                              disabled={loadingArticles}
                              required
                            >
                              <option value="">-- Sélectionner un article --</option>
                              {articles.map((a) => (
                                <option key={a.id} value={a.id}>
                                  {a.reference} - {a.designation} (Stock: {a.stockQuantity})
                                </option>
                              ))}
                            </select>
                            {line.articleId > 0 && (
                              <div className="flex items-center gap-2 mt-1 text-[11px]">
                                <span className="text-muted-foreground">
                                  Stock dispo: {formatQuantity(line.availableStock ?? 0)}
                                </span>
                                {hasStockWarning && (
                                  <span className="text-amber-600 font-medium">
                                    (Stock insuffisant pour confirmation immédiate)
                                  </span>
                                )}
                              </div>
                            )}
                          </td>
                          <td className="p-2">
                            <Input
                              type="number"
                              min="0.0001"
                              step="any"
                              className="h-8 text-xs"
                              value={line.quantity}
                              onChange={(e) =>
                                handleQuantityChange(idx, Number(e.target.value))
                              }
                              required
                            />
                          </td>
                          <td className="p-2">
                            <Input
                              type="number"
                              min="0"
                              step="0.01"
                              className="h-8 text-xs"
                              value={line.unitPrice}
                              onChange={(e) =>
                                handleUnitPriceChange(idx, Number(e.target.value))
                              }
                              required
                            />
                          </td>
                          <td className="p-2 text-right font-medium text-xs">
                            {formatCurrency(lineTotalHt)}
                          </td>
                          <td className="p-2 text-center">
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              disabled={lines.length <= 1}
                              onClick={() => handleRemoveLine(idx)}
                              className="h-7 w-7 p-0 text-muted-foreground hover:text-destructive"
                            >
                              <HugeiconsIcon icon={Delete02Icon} className="size-4" />
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Notes & Summary */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              <div>
                <Field>
                  <FieldLabel>Notes / Conditions particulières</FieldLabel>
                  <Textarea
                    placeholder="Instructions de livraison, modalité de paiement..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={3}
                  />
                </Field>
              </div>

              <div className="bg-muted/40 rounded-lg p-3 space-y-1.5 text-sm">
                <div className="flex justify-between text-muted-foreground">
                  <span>Sous-total HT:</span>
                  <span className="font-medium text-foreground">
                    {formatCurrency(subtotalHt)}
                  </span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>TVA ({taxRate}%):</span>
                  <span className="font-medium text-foreground">
                    {formatCurrency(taxAmount)}
                  </span>
                </div>
                <div className="border-t pt-1.5 flex justify-between font-semibold text-base">
                  <span>Total TTC:</span>
                  <span className="text-primary">{formatCurrency(totalTtc)}</span>
                </div>
              </div>
            </div>
          </DialogPanel>

          <DialogFooter className="mt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Annuler
            </Button>
            <Button type="submit" disabled={createOrderMutation.isPending}>
              {createOrderMutation.isPending
                ? "Enregistrement..."
                : "Créer la commande (Brouillon)"}
            </Button>
          </DialogFooter>
        </form>
      </DialogPopup>
    </Dialog>
  );
}
