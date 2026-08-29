"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
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
import { useUpdateUnit } from "@/hooks/use-units";
import { getApiErrorMessage } from "@/lib/api-error";
import type { UnitResponse } from "@/types/models";

interface EditUnitDialogProps {
  unit: UnitResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  name: "",
  symbol: "",
};

export function EditUnitDialog({
  unit,
  open,
  onOpenChange,
}: EditUnitDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loadedUnitId, setLoadedUnitId] = useState<number | null>(null);
  const updateUnit = useUpdateUnit();

  if (unit && loadedUnitId !== unit.id) {
    setLoadedUnitId(unit.id);
    setForm({
      name: unit.name,
      symbol: unit.symbol,
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!unit || !form.name.trim() || !form.symbol.trim()) return;
    setError(null);

    try {
      await updateUnit.mutateAsync({
        id: unit.id,
        data: {
          name: form.name.trim(),
          symbol: form.symbol.trim(),
        },
      });
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de modifier l'unité."));
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) setError(null);
      }}
    >
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>Modifier l'unité</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-unit-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Field>
              <FieldLabel htmlFor="edit-name">Nom</FieldLabel>
              <Input
                id="edit-name"
                required
                placeholder="Kilogramme, Litre, Pièce..."
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-symbol">Symbole</FieldLabel>
              <Input
                id="edit-symbol"
                required
                placeholder="kg, L, pce..."
                value={form.symbol}
                onChange={(e) => setForm({ ...form, symbol: e.target.value })}
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
              form="edit-unit-form"
              loading={updateUnit.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
