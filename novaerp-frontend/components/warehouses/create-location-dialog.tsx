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
import { Textarea } from "@/components/ui/textarea";
import { useCreateWarehouseLocation } from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";

interface CreateLocationDialogProps {
  warehouseId: number | null;
  warehouseName?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  code: "",
  name: "",
  description: "",
};

export function CreateLocationDialog({
  warehouseId,
  warehouseName,
  open,
  onOpenChange,
}: CreateLocationDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createLocation = useCreateWarehouseLocation(warehouseId ?? 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!warehouseId || warehouseId <= 0) {
      setError("Entrepôt non sélectionné ou invalide.");
      return;
    }

    const code = form.code.trim();
    const name = form.name.trim();
    const description = form.description.trim();

    if (!code) {
      setError("Le code de l'emplacement est requis.");
      return;
    }
    if (code.length > 50) {
      setError("Le code de l'emplacement ne peut pas dépasser 50 caractères.");
      return;
    }
    if (!name) {
      setError("Le nom de l'emplacement est requis.");
      return;
    }
    if (name.length > 255) {
      setError("Le nom de l'emplacement ne peut pas dépasser 255 caractères.");
      return;
    }
    if (description.length > 500) {
      setError("La description ne peut pas dépasser 500 caractères.");
      return;
    }

    setError(null);

    try {
      await createLocation.mutateAsync({
        code,
        name,
        description: description || null,
      });
      setForm(emptyForm);
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer l'emplacement."));
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
          <DialogTitle>
            Nouvel emplacement {warehouseName ? `(${warehouseName})` : ""}
          </DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-location-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Field>
              <FieldLabel htmlFor="location-code">Code</FieldLabel>
              <Input
                id="location-code"
                required
                maxLength={50}
                placeholder="ex. LOC-A-01"
                value={form.code}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, code: e.target.value }))
                }
                disabled={createLocation.isPending}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="location-name">Nom</FieldLabel>
              <Input
                id="location-name"
                required
                maxLength={255}
                placeholder="ex. Allée A, Étagère 1"
                value={form.name}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, name: e.target.value }))
                }
                disabled={createLocation.isPending}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="location-description">Description</FieldLabel>
              <Textarea
                id="location-description"
                maxLength={500}
                placeholder="Zone de stockage spécifique, type de marchandise..."
                value={form.description}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, description: e.target.value }))
                }
                disabled={createLocation.isPending}
              />
            </Field>
          </DialogPanel>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={createLocation.isPending}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              form="create-location-form"
              loading={createLocation.isPending}
              disabled={createLocation.isPending || !warehouseId}
            >
              Créer l&apos;emplacement
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
