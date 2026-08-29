"use client";

import type React from "react";
import { useState } from "react";
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
  const createClients = useCreateClients();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.nom.trim()) return;

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
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        onOpenChange(next);
        if (!next) setForm(emptyForm);
      }}
    >
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>Nouveau client</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-client-form">
          <DialogPanel className="flex flex-col gap-4">
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
