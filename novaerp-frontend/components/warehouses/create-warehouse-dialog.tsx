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
import { useCreateWarehouse } from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";

interface CreateWarehouseDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  code: "",
  name: "",
  description: "",
  address: "",
};

export function CreateWarehouseDialog({
  open,
  onOpenChange,
}: CreateWarehouseDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createWarehouse = useCreateWarehouse();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
      await createWarehouse.mutateAsync({
        code,
        name,
        description: description || null,
        address: address || null,
      });
      setForm(emptyForm);
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer l'entrepôt."));
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
      <DialogPopup className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Nouvel entrepôt</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-warehouse-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="warehouse-code">Code</FieldLabel>
                <Input
                  id="warehouse-code"
                  required
                  maxLength={50}
                  placeholder="ex. WH-CAS-01"
                  value={form.code}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, code: e.target.value }))
                  }
                  disabled={createWarehouse.isPending}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="warehouse-name">Nom</FieldLabel>
                <Input
                  id="warehouse-name"
                  required
                  maxLength={255}
                  placeholder="ex. Entrepôt Principal"
                  value={form.name}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, name: e.target.value }))
                  }
                  disabled={createWarehouse.isPending}
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="warehouse-address">Adresse</FieldLabel>
              <Input
                id="warehouse-address"
                maxLength={255}
                placeholder="ex. Zone Industrielle Tit Mellil, Casablanca"
                value={form.address}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, address: e.target.value }))
                }
                disabled={createWarehouse.isPending}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="warehouse-description">Description</FieldLabel>
              <Textarea
                id="warehouse-description"
                maxLength={500}
                placeholder="Description ou notes concernant cet entrepôt"
                value={form.description}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, description: e.target.value }))
                }
                disabled={createWarehouse.isPending}
              />
            </Field>
          </DialogPanel>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={createWarehouse.isPending}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              form="create-warehouse-form"
              loading={createWarehouse.isPending}
              disabled={createWarehouse.isPending}
            >
              Créer l&apos;entrepôt
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
