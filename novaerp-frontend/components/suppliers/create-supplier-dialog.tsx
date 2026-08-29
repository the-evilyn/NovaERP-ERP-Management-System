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
import { useCreateSupplier } from "@/hooks/use-suppliers";
import { getApiErrorMessage } from "@/lib/api-error";

interface CreateSupplierDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  name: "",
  email: "",
  phone: "",
  address: "",
};

export function CreateSupplierDialog({
  open,
  onOpenChange,
}: CreateSupplierDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createSupplier = useCreateSupplier();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setError(null);

    try {
      await createSupplier.mutateAsync({
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        address: form.address.trim(),
      });
      setForm(emptyForm);
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer le fournisseur."));
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
          <DialogTitle>Nouveau fournisseur</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-supplier-form">
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
                placeholder="Nom du fournisseur"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                placeholder="contact@fournisseur.ma"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="phone">Téléphone</FieldLabel>
              <Input
                id="phone"
                placeholder="0600000000"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="address">Adresse</FieldLabel>
              <Input
                id="address"
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
              form="create-supplier-form"
              loading={createSupplier.isPending}
            >
              Créer le fournisseur
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
