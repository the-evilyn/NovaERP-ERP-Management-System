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
import { useUpdateWarehouse } from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import type { WarehouseResponse } from "@/types/models";

interface EditWarehouseDialogProps {
  warehouse: WarehouseResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  code: "",
  name: "",
  description: "",
  address: "",
};

export function EditWarehouseDialog({
  warehouse,
  open,
  onOpenChange,
}: EditWarehouseDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loadedWarehouseId, setLoadedWarehouseId] = useState<number | null>(
    null,
  );
  const updateWarehouse = useUpdateWarehouse();

  if (warehouse && loadedWarehouseId !== warehouse.id) {
    setLoadedWarehouseId(warehouse.id);
    setForm({
      code: warehouse.code,
      name: warehouse.name,
      description: warehouse.description ?? "",
      address: warehouse.address ?? "",
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!warehouse) return;

    const code = form.code.trim();
    const name = form.name.trim();
    const description = form.description.trim();
    const address = form.address.trim();

    if (!code) {
      setError("Le code de l'entrepôt est requis.");
      return;
    }
    if (code.length > 50) {
      setError("Le code de l'entrepôt ne peut pas dépasser 50 caractères.");
      return;
    }
    if (!name) {
      setError("Le nom de l'entrepôt est requis.");
      return;
    }
    if (name.length > 255) {
      setError("Le nom de l'entrepôt ne peut pas dépasser 255 caractères.");
      return;
    }
    if (description.length > 500) {
      setError("La description ne peut pas dépasser 500 caractères.");
      return;
    }
    if (address.length > 255) {
      setError("L'adresse ne peut pas dépasser 255 caractères.");
      return;
    }

    setError(null);

    try {
      await updateWarehouse.mutateAsync({
        id: warehouse.id,
        data: {
          code,
          name,
          description: description || null,
          address: address || null,
        },
      });
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de modifier l'entrepôt."));
    }
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) {
          setError(null);
          setLoadedWarehouseId(null);
        }
      }}
    >
      <DialogPopup className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Modifier l&apos;entrepôt</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-warehouse-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="edit-warehouse-code">Code</FieldLabel>
                <Input
                  id="edit-warehouse-code"
                  required
                  maxLength={50}
                  placeholder="ex. WH-CAS-01"
                  value={form.code}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, code: e.target.value }))
                  }
                  disabled={updateWarehouse.isPending}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="edit-warehouse-name">Nom</FieldLabel>
                <Input
                  id="edit-warehouse-name"
                  required
                  maxLength={255}
                  placeholder="ex. Entrepôt Principal"
                  value={form.name}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, name: e.target.value }))
                  }
                  disabled={updateWarehouse.isPending}
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="edit-warehouse-address">Adresse</FieldLabel>
              <Input
                id="edit-warehouse-address"
                maxLength={255}
                placeholder="ex. Zone Industrielle Tit Mellil, Casablanca"
                value={form.address}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, address: e.target.value }))
                }
                disabled={updateWarehouse.isPending}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-warehouse-description">Description</FieldLabel>
              <Textarea
                id="edit-warehouse-description"
                maxLength={500}
                placeholder="Description ou notes concernant cet entrepôt"
                value={form.description}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, description: e.target.value }))
                }
                disabled={updateWarehouse.isPending}
              />
            </Field>
          </DialogPanel>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={updateWarehouse.isPending}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              form="edit-warehouse-form"
              loading={updateWarehouse.isPending}
              disabled={updateWarehouse.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
