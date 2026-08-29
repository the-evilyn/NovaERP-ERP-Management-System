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
import { useUpdateSupplier } from "@/hooks/use-suppliers";
import { getApiErrorMessage } from "@/lib/api-error";
import type { SupplierResponse } from "@/types/models";

interface EditSupplierDialogProps {
  supplier: SupplierResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  name: "",
  email: "",
  phone: "",
  address: "",
};

export function EditSupplierDialog({
  supplier,
  open,
  onOpenChange,
}: EditSupplierDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loadedSupplierId, setLoadedSupplierId] = useState<number | null>(
    null,
  );
  const updateSupplier = useUpdateSupplier();

  if (supplier && loadedSupplierId !== supplier.id) {
    setLoadedSupplierId(supplier.id);
    setForm({
      name: supplier.name,
      email: supplier.email ?? "",
      phone: supplier.phone ?? "",
      address: supplier.address ?? "",
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supplier || !form.name.trim()) return;
    setError(null);

    try {
      await updateSupplier.mutateAsync({
        id: supplier.id,
        data: {
          name: form.name.trim(),
          email: form.email.trim(),
          phone: form.phone.trim(),
          address: form.address.trim(),
        },
      });
      onOpenChange(false);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "Impossible de modifier le fournisseur."),
      );
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
          <DialogTitle>Modifier le fournisseur</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-supplier-form">
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
                placeholder="Nom du fournisseur"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-email">Email</FieldLabel>
              <Input
                id="edit-email"
                type="email"
                placeholder="contact@fournisseur.ma"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-phone">Téléphone</FieldLabel>
              <Input
                id="edit-phone"
                placeholder="0600000000"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-address">Adresse</FieldLabel>
              <Input
                id="edit-address"
                placeholder="Adresse du fournisseur"
                value={form.address}
                onChange={(e) =>
                  setForm({ ...form, address: e.target.value })
                }
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
              form="edit-supplier-form"
              loading={updateSupplier.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
