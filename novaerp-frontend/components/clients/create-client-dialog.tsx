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
import { useCreateClients } from "@/hooks/use-clients";
import { getApiErrorMessage } from "@/lib/api-error";
import { toastManager } from "@/components/ui/toast";

interface CreateClientDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  nom: "",
  email: "",
  telephone: "",
  adresse: "",
};

export function CreateClientDialog({
  open,
  onOpenChange,
}: CreateClientDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createClients = useCreateClients();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.nom.trim()) return;
    setError(null);

    try {
      await createClients.mutateAsync([
        {
          nom: form.nom.trim(),
          email: form.email.trim() || null,
          telephone: form.telephone.trim() || null,
          adresse: form.adresse.trim() || null,
        },
      ]);

      setForm(emptyForm);
      onOpenChange(false);
      toastManager.add({
        title: "Client créé",
        description: "Le client a été créé avec succès.",
        type: "success",
      });
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de créer le client."));
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
          <DialogTitle>Nouveau client</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-client-form">
          <DialogPanel className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Field>
              <FieldLabel htmlFor="nom">Nom</FieldLabel>
              <Input
                id="nom"
                required
                placeholder="Nom du client"
                value={form.nom}
                onChange={(e) => setForm({ ...form, nom: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                placeholder="contact@exemple.ma"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="telephone">Téléphone</FieldLabel>
              <Input
                id="telephone"
                placeholder="0600000000"
                value={form.telephone}
                onChange={(e) =>
                  setForm({ ...form, telephone: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="adresse">Adresse</FieldLabel>
              <Input
                id="adresse"
                placeholder="Adresse du client"
                value={form.adresse}
                onChange={(e) => setForm({ ...form, adresse: e.target.value })}
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
              form="create-client-form"
              loading={createClients.isPending}
            >
              Créer le client
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
