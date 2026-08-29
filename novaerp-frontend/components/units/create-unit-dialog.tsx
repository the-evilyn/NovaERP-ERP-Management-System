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
import { useCreateUnit } from "@/hooks/use-units";
import { getApiErrorMessage } from "@/lib/api-error";

interface CreateUnitDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  name: "",
  symbol: "",
};

export function CreateUnitDialog({
  open,
  onOpenChange,
}: CreateUnitDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createUnit = useCreateUnit();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.symbol.trim()) return;
    setError(null);

    try {
      await createUnit.mutateAsync({
        name: form.name.trim(),
        symbol: form.symbol.trim(),
      });
      setForm(emptyForm);
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer l'unité."));
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) {
          setForm(emptyForm);
          setError(null);
        }
      }}
    >
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>Nouvelle unité</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-unit-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Field>
              <FieldLabel htmlFor="name">Nom</FieldLabel>
              <Input
                id="name"
                required
                placeholder="Kilogramme, Litre, Pièce..."
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="symbol">Symbole</FieldLabel>
              <Input
                id="symbol"
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
              form="create-unit-form"
              loading={createUnit.isPending}
            >
              Créer l'unité
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
