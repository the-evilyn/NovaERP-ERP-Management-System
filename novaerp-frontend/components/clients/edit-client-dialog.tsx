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
import { useUpdateClient } from "@/hooks/use-clients";
import type { Client } from "@/types/models";

interface EditClientDialogProps {
  client: Client | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  nom: "",
  email: "",
  telephone: "",
  adresse: "",
};

export function EditClientDialog({
  client,
  open,
  onOpenChange,
}: EditClientDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [loadedClientId, setLoadedClientId] = useState<number | null>(null);
  const updateClient = useUpdateClient();

  if (client && loadedClientId !== client.id) {
    setLoadedClientId(client.id);
    setForm({
      nom: client.nom,
      email: client.email ?? "",
      telephone: client.telephone ?? "",
      adresse: client.adresse ?? "",
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!client || !form.nom.trim()) return;

    await updateClient.mutateAsync({
      id: client.id,
      data: {
        nom: form.nom.trim(),
        email: form.email.trim() || null,
        telephone: form.telephone.trim() || null,
        adresse: form.adresse.trim() || null,
      },
    });

    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>Modifier le client</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-client-form">
          <DialogPanel className="flex flex-col gap-4">
            <Field>
              <FieldLabel htmlFor="edit-nom">Nom</FieldLabel>
              <Input
                id="edit-nom"
                required
                placeholder="Nom du client"
                value={form.nom}
                onChange={(e) => setForm({ ...form, nom: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-email">Email</FieldLabel>
              <Input
                id="edit-email"
                type="email"
                placeholder="contact@exemple.ma"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-telephone">Téléphone</FieldLabel>
              <Input
                id="edit-telephone"
                placeholder="0600000000"
                value={form.telephone}
                onChange={(e) =>
                  setForm({ ...form, telephone: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-adresse">Adresse</FieldLabel>
              <Input
                id="edit-adresse"
                placeholder="Adresse du client"
                value={form.adresse}
                onChange={(e) =>
                  setForm({ ...form, adresse: e.target.value })
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
              form="edit-client-form"
              loading={updateClient.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
