"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
  AlertCircleIcon,
  ArrowLeft01Icon,
  CheckmarkCircle02Icon,
  Delete02Icon,
} from "@hugeicons/core-free-icons";
import { useRouter } from "next/navigation";
import type React from "react";
import { useMemo, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { useArticles } from "@/hooks/use-articles";
import {
  useCreateStockTransfer,
  useUpdateStockTransfer,
} from "@/hooks/use-stock-transfers";
import {
  useWarehouseLocations,
  useWarehouses,
} from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import type {
  StockTransferRequest,
  StockTransferResponse,
} from "@/types/models";

interface FormItemLine {
  articleId: number | "";
  quantity: number | "";
}

interface StockTransferFormProps {
  initialTransfer?: StockTransferResponse;
  isEdit?: boolean;
}

export function StockTransferForm({
  initialTransfer,
  isEdit = false,
}: StockTransferFormProps): React.ReactElement {
  const router = useRouter();

  const [sourceWarehouseId, setSourceWarehouseId] = useState<number | "">(
    initialTransfer?.sourceWarehouseId ?? "",
  );
  const [sourceLocationId, setSourceLocationId] = useState<number | "">(
    initialTransfer?.sourceLocationId ?? "",
  );
  const [destinationWarehouseId, setDestinationWarehouseId] = useState<number | "">(
    initialTransfer?.destinationWarehouseId ?? "",
  );
  const [destinationLocationId, setDestinationLocationId] = useState<number | "">(
    initialTransfer?.destinationLocationId ?? "",
  );
  const [notes, setNotes] = useState<string>(initialTransfer?.notes ?? "");

  const [items, setItems] = useState<FormItemLine[]>(() => {
    if (initialTransfer?.items && initialTransfer.items.length > 0) {
      return initialTransfer.items.map((it) => ({
        articleId: it.articleId,
        quantity: it.quantity,
      }));
    }
    return [{ articleId: "", quantity: 1 }];
  });

  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Queries for active warehouses and locations
  const { data: warehousesData, isLoading: loadingWarehouses } = useWarehouses(
    0,
    100,
    true,
  );
  const warehouses = warehousesData?.content ?? [];

  const { data: srcLocationsData } = useWarehouseLocations(
    typeof sourceWarehouseId === "number" ? sourceWarehouseId : 0,
    0,
    100,
    true,
  );
  const sourceLocations = srcLocationsData?.content ?? [];

  const { data: dstLocationsData } = useWarehouseLocations(
    typeof destinationWarehouseId === "number" ? destinationWarehouseId : 0,
    0,
    100,
    true,
  );
  const destinationLocations = dstLocationsData?.content ?? [];

  // Articles query
  const { data: articlesData, isLoading: loadingArticles } = useArticles(0, 500);
  const articles = articlesData?.content ?? [];

  const createMutation = useCreateStockTransfer();
  const updateMutation = useUpdateStockTransfer();

  // Reset location when warehouse changes
  const handleSourceWarehouseChange = (val: string) => {
    const nextId = val ? Number(val) : "";
    setSourceWarehouseId(nextId);
    setSourceLocationId("");
  };

  const handleDestinationWarehouseChange = (val: string) => {
    const nextId = val ? Number(val) : "";
    setDestinationWarehouseId(nextId);
    setDestinationLocationId("");
  };

  // Item lines management
  const handleAddLine = () => {
    setItems((prev) => [...prev, { articleId: "", quantity: 1 }]);
  };

  const handleRemoveLine = (index: number) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleItemArticleChange = (index: number, val: string) => {
    const articleId = val ? Number(val) : "";
    setItems((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], articleId };
      return copy;
    });
  };

  const handleItemQuantityChange = (index: number, val: string) => {
    const qty = val === "" ? "" : Number(val);
    setItems((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], quantity: qty };
      return copy;
    });
  };

  // Detect duplicate articles
  const duplicateArticleIds = useMemo(() => {
    const counts = new Map<number, number>();
    for (const item of items) {
      if (typeof item.articleId === "number" && item.articleId > 0) {
        counts.set(item.articleId, (counts.get(item.articleId) ?? 0) + 1);
      }
    }
    const duplicates = new Set<number>();
    for (const [id, count] of counts.entries()) {
      if (count > 1) duplicates.add(id);
    }
    return duplicates;
  }, [items]);

  // Same warehouse + same location restriction check
  const isSameWarehouseAndLocation = useMemo(() => {
    if (
      typeof sourceWarehouseId === "number" &&
      typeof destinationWarehouseId === "number" &&
      sourceWarehouseId === destinationWarehouseId
    ) {
      const srcLoc = sourceLocationId === "" ? null : sourceLocationId;
      const dstLoc = destinationLocationId === "" ? null : destinationLocationId;
      return srcLoc === dstLoc;
    }
    return false;
  }, [
    sourceWarehouseId,
    destinationWarehouseId,
    sourceLocationId,
    destinationLocationId,
  ]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    // Form validations
    if (sourceWarehouseId === "") {
      setErrorMsg("Veuillez sélectionner l'entrepôt source.");
      return;
    }
    if (destinationWarehouseId === "") {
      setErrorMsg("Veuillez sélectionner l'entrepôt de destination.");
      return;
    }
    if (isSameWarehouseAndLocation) {
      setErrorMsg(
        "L'emplacement source et l'emplacement de destination ne peuvent pas être identiques au sein du même entrepôt.",
      );
      return;
    }

    if (items.length === 0) {
      setErrorMsg("Le transfert doit comporter au moins un article.");
      return;
    }

    if (duplicateArticleIds.size > 0) {
      setErrorMsg(
        "Des articles en double ont été détectés. Chaque article ne doit apparaître qu'une seule fois dans la liste.",
      );
      return;
    }

    for (let i = 0; i < items.length; i++) {
      const line = items[i];
      if (line.articleId === "" || line.articleId <= 0) {
        setErrorMsg(`Ligne ${i + 1} : Veuillez sélectionner un article.`);
        return;
      }
      if (typeof line.quantity !== "number" || line.quantity <= 0) {
        setErrorMsg(`Ligne ${i + 1} : La quantité doit être strictement positive.`);
        return;
      }
    }

    const payload: StockTransferRequest = {
      sourceWarehouseId: Number(sourceWarehouseId),
      sourceLocationId: sourceLocationId !== "" ? Number(sourceLocationId) : null,
      destinationWarehouseId: Number(destinationWarehouseId),
      destinationLocationId:
        destinationLocationId !== "" ? Number(destinationLocationId) : null,
      notes: notes.trim() || null,
      items: items.map((it) => ({
        articleId: Number(it.articleId),
        quantity: Number(it.quantity),
      })),
    };

    setIsSubmitting(true);
    try {
      if (isEdit && initialTransfer) {
        await updateMutation.mutateAsync({
          id: initialTransfer.id,
          data: payload,
        });
        router.push(`/stock/transfers/${initialTransfer.id}`);
      } else {
        const created = await createMutation.mutateAsync(payload);
        router.push(`/stock/transfers/${created.id}`);
      }
    } catch (err) {
      setIsSubmitting(false);
      setErrorMsg(getApiErrorMessage(err));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={() => router.back()}
          >
            <HugeiconsIcon icon={ArrowLeft01Icon} className="size-4" />
          </Button>
          <div>
            <h1 className="font-bold text-xl tracking-tight">
              {isEdit
                ? `Modifier le transfert ${initialTransfer?.transferNumber}`
                : "Nouveau transfert de stock"}
            </h1>
            <p className="text-muted-foreground text-xs">
              {isEdit
                ? "Mettre à jour les entrepôts, emplacements et articles du transfert en brouillon."
                : "Créer un nouveau transfert en statut brouillon avant validation."}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => router.back()}
            disabled={isSubmitting}
          >
            Annuler
          </Button>
          <Button
            type="submit"
            size="sm"
            disabled={isSubmitting || isSameWarehouseAndLocation || duplicateArticleIds.size > 0}
            className="gap-1.5"
          >
            {isSubmitting ? (
              <>
                <Spinner className="size-3.5" />
                Enregistrement...
              </>
            ) : (
              <>
                <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3.5" />
                {isEdit ? "Mettre à jour le brouillon" : "Enregistrer le brouillon"}
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Global Error Banner */}
      {errorMsg && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
          <AlertDescription className="text-xs">{errorMsg}</AlertDescription>
        </Alert>
      )}

      {/* Same warehouse & same location warning */}
      {isSameWarehouseAndLocation && (
        <Alert variant="warning">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
          <AlertDescription className="text-xs">
            L&apos;emplacement source et l&apos;emplacement de destination ne peuvent pas être identiques au sein du même entrepôt. Choisissez un emplacement différent pour effectuer un transfert interne.
          </AlertDescription>
        </Alert>
      )}

      {/* Warehouses and Locations Card */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Source Card */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <span className="flex size-2 rounded-full bg-amber-500" />
              Origine / Source
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <Field>
              <FieldLabel>Entrepôt source *</FieldLabel>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                value={sourceWarehouseId}
                onChange={(e) => handleSourceWarehouseChange(e.target.value)}
                disabled={loadingWarehouses || isSubmitting}
                required
              >
                <option value="">
                  {loadingWarehouses
                    ? "Chargement des entrepôts..."
                    : "-- Sélectionner l'entrepôt source --"}
                </option>
                {warehouses.map((wh) => (
                  <option key={wh.id} value={wh.id}>
                    {wh.code} - {wh.name}
                  </option>
                ))}
              </select>
            </Field>

            <Field>
              <FieldLabel>Emplacement source (Optionnel)</FieldLabel>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                value={sourceLocationId}
                onChange={(e) =>
                  setSourceLocationId(e.target.value ? Number(e.target.value) : "")
                }
                disabled={sourceWarehouseId === "" || isSubmitting}
              >
                <option value="">
                  {sourceWarehouseId === ""
                    ? "Sélectionnez d'abord l'entrepôt source"
                    : "-- Emplacement par défaut (LOC-GEN) --"}
                </option>
                {sourceLocations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.code} - {loc.name} {loc.isDefault ? "(Par défaut)" : ""}
                  </option>
                ))}
              </select>
            </Field>
          </CardContent>
        </Card>

        {/* Destination Card */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <span className="flex size-2 rounded-full bg-emerald-500" />
              Destination
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-xs">
            <Field>
              <FieldLabel>Entrepôt destination *</FieldLabel>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                value={destinationWarehouseId}
                onChange={(e) => handleDestinationWarehouseChange(e.target.value)}
                disabled={loadingWarehouses || isSubmitting}
                required
              >
                <option value="">
                  {loadingWarehouses
                    ? "Chargement des entrepôts..."
                    : "-- Sélectionner l'entrepôt destination --"}
                </option>
                {warehouses.map((wh) => (
                  <option key={wh.id} value={wh.id}>
                    {wh.code} - {wh.name}
                  </option>
                ))}
              </select>
            </Field>

            <Field>
              <FieldLabel>Emplacement destination (Optionnel)</FieldLabel>
              <select
                className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                value={destinationLocationId}
                onChange={(e) =>
                  setDestinationLocationId(e.target.value ? Number(e.target.value) : "")
                }
                disabled={destinationWarehouseId === "" || isSubmitting}
              >
                <option value="">
                  {destinationWarehouseId === ""
                    ? "Sélectionnez d'abord l'entrepôt destination"
                    : "-- Emplacement par défaut (LOC-GEN) --"}
                </option>
                {destinationLocations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.code} - {loc.name} {loc.isDefault ? "(Par défaut)" : ""}
                  </option>
                ))}
              </select>
            </Field>
          </CardContent>
        </Card>
      </div>

      {/* Notes Field */}
      <Card>
        <CardContent className="pt-4">
          <Field>
            <FieldLabel>Notes / Observations (Optionnel)</FieldLabel>
            <Textarea
              placeholder="Motif du transfert, instructions logistiques, etc."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              maxLength={1000}
              rows={2}
              className="text-xs"
              disabled={isSubmitting}
            />
          </Field>
        </CardContent>
      </Card>

      {/* Items Section */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div>
            <CardTitle className="text-sm font-semibold">
              Articles à transférer *
            </CardTitle>
            <p className="text-muted-foreground text-xs">
              Ajoutez les articles et quantités strictement positives à déplacer.
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleAddLine}
            disabled={isSubmitting}
            className="h-8 text-xs gap-1"
          >
            <HugeiconsIcon icon={Add01Icon} className="size-3.5" />
            Ajouter un article
          </Button>
        </CardHeader>

        <CardContent className="space-y-3">
          {duplicateArticleIds.size > 0 && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
              <AlertDescription className="text-xs">
                Certains articles apparaissent en double. Veuillez regrouper leurs quantités.
              </AlertDescription>
            </Alert>
          )}

          <div className="border rounded-lg overflow-x-auto">
            <table className="w-full text-xs">
              <thead className="bg-muted/50 text-muted-foreground border-b">
                <tr>
                  <th className="p-2.5 text-left w-12">#</th>
                  <th className="p-2.5 text-left">Article *</th>
                  <th className="p-2.5 text-left w-32">Unité</th>
                  <th className="p-2.5 text-right w-36">Quantité *</th>
                  <th className="p-2.5 text-center w-16">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {items.map((line, index) => {
                  const selectedArticle = articles.find((a) => a.id === line.articleId);
                  const isDuplicate =
                    typeof line.articleId === "number" &&
                    duplicateArticleIds.has(line.articleId);

                  return (
                    <tr
                      key={index}
                      className={isDuplicate ? "bg-destructive/5" : undefined}
                    >
                      <td className="p-2.5 text-muted-foreground font-mono">
                        {index + 1}
                      </td>
                      <td className="p-2.5">
                        <select
                          className={`flex h-8 w-full rounded-md border bg-background px-2.5 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring ${
                            isDuplicate
                              ? "border-destructive text-destructive"
                              : "border-input"
                          }`}
                          value={line.articleId}
                          onChange={(e) =>
                            handleItemArticleChange(index, e.target.value)
                          }
                          disabled={loadingArticles || isSubmitting}
                          required
                        >
                          <option value="">
                            {loadingArticles
                              ? "Chargement des articles..."
                              : "-- Sélectionner un article --"}
                          </option>
                          {articles.map((art) => (
                            <option key={art.id} value={art.id}>
                              {art.reference} - {art.designation}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="p-2.5 text-muted-foreground">
                        {selectedArticle?.unitName ?? "—"}
                      </td>
                      <td className="p-2.5 text-right">
                        <Input
                          type="number"
                          min="0.0001"
                          step="0.0001"
                          className="h-8 text-right text-xs"
                          placeholder="0.0000"
                          value={line.quantity}
                          onChange={(e) =>
                            handleItemQuantityChange(index, e.target.value)
                          }
                          disabled={isSubmitting}
                          required
                        />
                      </td>
                      <td className="p-2.5 text-center">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-xs"
                          disabled={items.length <= 1 || isSubmitting}
                          onClick={() => handleRemoveLine(index)}
                          className="text-muted-foreground hover:text-destructive"
                        >
                          <HugeiconsIcon icon={Delete02Icon} className="size-3.5" />
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </form>
  );
}
