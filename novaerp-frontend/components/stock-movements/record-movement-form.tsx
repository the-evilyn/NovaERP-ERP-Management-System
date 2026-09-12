"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useMemo, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Field, FieldLabel } from "@/components/ui/field";
import { Form } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectItem,
  SelectPopup,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCreateStockMovement } from "@/hooks/use-stock-movements";
import {
  useWarehouseLocations,
  useWarehouses,
} from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ArticleResponse, StockMovementType } from "@/types/models";

interface RecordMovementFormProps {
  article: ArticleResponse;
}

const movementTypeOptions: { label: string; value: StockMovementType }[] = [
  { label: "Entrée", value: "IN" },
  { label: "Sortie", value: "OUT" },
  { label: "Ajustement", value: "ADJUSTMENT" },
];

const emptyForm = {
  type: "IN" as StockMovementType,
  quantity: "",
  reference: "",
  note: "",
};

export function RecordMovementForm({
  article,
}: RecordMovementFormProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [userWarehouseId, setUserWarehouseId] = useState<number | null>(null);
  const [userLocationId, setUserLocationId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const createMovement = useCreateStockMovement();

  // Load only active warehouses
  const { data: warehousesPage, isPending: warehousesLoading } = useWarehouses(
    0,
    100,
    true,
  );
  const warehouses = useMemo(
    () => warehousesPage?.content ?? [],
    [warehousesPage?.content],
  );

  const defaultWarehouse = useMemo(
    () => warehouses.find((w) => w.isDefault) ?? warehouses[0] ?? null,
    [warehouses],
  );

  const warehouseId = userWarehouseId ?? defaultWarehouse?.id ?? null;

  // Load only active locations for the selected warehouse
  const { data: locationsPage, isPending: locationsLoading } =
    useWarehouseLocations(warehouseId ?? 0, 0, 100, true);
  const locations = useMemo(
    () => locationsPage?.content ?? [],
    [locationsPage?.content],
  );

  const defaultLocation = useMemo(
    () => locations.find((l) => l.isDefault) ?? locations[0] ?? null,
    [locations],
  );

  // Ensure location always belongs to the selected warehouse
  const isUserLocationValid = useMemo(
    () => locations.some((l) => l.id === userLocationId),
    [locations, userLocationId],
  );

  const locationId = isUserLocationValid
    ? userLocationId
    : (defaultLocation?.id ?? null);

  const handleWarehouseChange = (newWarehouseId: number | null) => {
    setUserWarehouseId(newWarehouseId);
    setUserLocationId(null); // Clear previous location selection immediately
    setError(null);
  };

  const handleLocationChange = (newLocationId: number | null) => {
    setUserLocationId(newLocationId);
    setError(null);
  };

  const warehouseItems = useMemo(
    () =>
      warehouses.map((w) => ({
        label: `${w.name} (${w.code})${w.isDefault ? " — Défaut" : ""}`,
        value: String(w.id),
      })),
    [warehouses],
  );

  const locationItems = useMemo(
    () =>
      locations.map((l) => ({
        label: `${l.name} (${l.code})${l.isDefault ? " — Défaut" : ""}`,
        value: String(l.id),
      })),
    [locations],
  );

  const hasNoLocations = Boolean(
    warehouseId && !locationsLoading && locations.length === 0,
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const quantity = Number(form.quantity);
    if (!quantity || quantity <= 0) {
      setError("La quantité doit être supérieure à 0.");
      return;
    }

    if (!warehouseId || warehouseId <= 0) {
      setError("Veuillez sélectionner un entrepôt valide.");
      return;
    }

    if (!locationId || locationId <= 0) {
      setError(
        "Un emplacement actif est requis avant d'enregistrer un mouvement.",
      );
      return;
    }

    setError(null);

    try {
      await createMovement.mutateAsync({
        articleId: article.id,
        type: form.type,
        quantity,
        reference: form.reference.trim(),
        note: form.note.trim(),
        warehouseId,
        locationId,
      });
      setForm(emptyForm);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "Impossible d'enregistrer le mouvement."),
      );
    }
  };

  return (
    <Card>
      <CardHeader>
        <h2 className="font-semibold text-sm">Nouveau mouvement</h2>
        <p className="text-muted-foreground text-xs">
          Stock actuel : {article.stockQuantity} {article.unitName ?? "u."}
        </p>
      </CardHeader>
      <CardContent>
        <Form onSubmit={handleSubmit} id="record-movement-form">
          <div className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            {hasNoLocations && (
              <Alert variant="warning">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>
                  Un emplacement actif est requis avant d&apos;enregistrer un
                  mouvement. Cet entrepôt n&apos;a aucun emplacement actif.
                </AlertDescription>
              </Alert>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="movement-type">Type</FieldLabel>
                <Select
                  items={movementTypeOptions}
                  value={form.type}
                  onValueChange={(value) =>
                    setForm({
                      ...form,
                      type: (value as StockMovementType) ?? "IN",
                    })
                  }
                >
                  <SelectTrigger id="movement-type">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectPopup>
                    {movementTypeOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>

              <Field>
                <FieldLabel htmlFor="movement-quantity">Quantité</FieldLabel>
                <Input
                  id="movement-quantity"
                  type="number"
                  min="1"
                  required
                  placeholder="0"
                  value={form.quantity}
                  onChange={(e) =>
                    setForm({ ...form, quantity: e.target.value })
                  }
                />
              </Field>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="movement-warehouse">Entrepôt</FieldLabel>
                <Select
                  items={warehouseItems}
                  value={warehouseId ? String(warehouseId) : null}
                  onValueChange={(value) =>
                    handleWarehouseChange(value ? Number(value) : null)
                  }
                  disabled={warehousesLoading || warehouses.length === 0}
                >
                  <SelectTrigger id="movement-warehouse">
                    <SelectValue
                      placeholder={
                        warehousesLoading
                          ? "Chargement..."
                          : warehouses.length === 0
                            ? "Aucun entrepôt disponible"
                            : "Sélectionner un entrepôt"
                      }
                    />
                  </SelectTrigger>
                  <SelectPopup>
                    {warehouseItems.map((item) => (
                      <SelectItem key={item.value} value={item.value}>
                        {item.label}
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>

              <Field>
                <FieldLabel htmlFor="movement-location">Emplacement</FieldLabel>
                <Select
                  items={locationItems}
                  value={locationId ? String(locationId) : null}
                  onValueChange={(value) =>
                    handleLocationChange(value ? Number(value) : null)
                  }
                  disabled={
                    !warehouseId ||
                    locationsLoading ||
                    locations.length === 0
                  }
                >
                  <SelectTrigger id="movement-location">
                    <SelectValue
                      placeholder={
                        !warehouseId
                          ? "Sélectionner un entrepôt d'abord"
                          : locationsLoading
                            ? "Chargement..."
                            : locations.length === 0
                              ? "Aucun emplacement disponible"
                              : "Sélectionner un emplacement"
                      }
                    />
                  </SelectTrigger>
                  <SelectPopup>
                    {locationItems.map((item) => (
                      <SelectItem key={item.value} value={item.value}>
                        {item.label}
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>
            </div>

            <Field>
              <FieldLabel htmlFor="movement-reference">Référence</FieldLabel>
              <Input
                id="movement-reference"
                placeholder="Bon de commande, facture..."
                value={form.reference}
                onChange={(e) =>
                  setForm({ ...form, reference: e.target.value })
                }
              />
            </Field>

            <Field>
              <FieldLabel htmlFor="movement-note">Note</FieldLabel>
              <Input
                id="movement-note"
                placeholder="Note optionnelle"
                value={form.note}
                onChange={(e) => setForm({ ...form, note: e.target.value })}
              />
            </Field>

            <Button
              type="submit"
              form="record-movement-form"
              loading={createMovement.isPending}
              disabled={
                createMovement.isPending ||
                !warehouseId ||
                !locationId ||
                hasNoLocations
              }
              className="self-start"
            >
              Enregistrer le mouvement
            </Button>
          </div>
        </Form>
      </CardContent>
    </Card>
  );
}
