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
import { useCreateProducts } from "@/hooks/use-products";

interface CreateProductDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  nom: "",
  reference: "",
  categorie: "",
  prixAchat: "",
  prixVente: "",
  quantiteStock: "",
  seuilMinimum: "",
};

export function CreateProductDialog({
  open,
  onOpenChange,
}: CreateProductDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const createProducts = useCreateProducts();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.nom.trim() || !form.reference.trim()) return;

    await createProducts.mutateAsync([
      {
        nom: form.nom.trim(),
        reference: form.reference.trim(),
        categorie: form.categorie.trim() || null,
        prixAchat: Number(form.prixAchat) || 0,
        prixVente: Number(form.prixVente) || 0,
        quantiteStock: Number(form.quantiteStock) || 0,
        seuilMinimum: Number(form.seuilMinimum) || 0,
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
          <DialogTitle>Nouveau produit</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="create-product-form">
          <DialogPanel className="flex flex-col gap-4">
            <Field>
              <FieldLabel htmlFor="nom">Nom</FieldLabel>
              <Input
                id="nom"
                required
                placeholder="Nom du produit"
                value={form.nom}
                onChange={(e) => setForm({ ...form, nom: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="reference">Référence (SKU)</FieldLabel>
              <Input
                id="reference"
                required
                placeholder="REF-001"
                value={form.reference}
                onChange={(e) =>
                  setForm({ ...form, reference: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="categorie">Catégorie</FieldLabel>
              <Input
                id="categorie"
                placeholder="Alimentation, Boissons..."
                value={form.categorie}
                onChange={(e) =>
                  setForm({ ...form, categorie: e.target.value })
                }
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="prixAchat">Prix d'achat</FieldLabel>
                <Input
                  id="prixAchat"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.prixAchat}
                  onChange={(e) =>
                    setForm({ ...form, prixAchat: e.target.value })
                  }
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="prixVente">Prix de vente</FieldLabel>
                <Input
                  id="prixVente"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.prixVente}
                  onChange={(e) =>
                    setForm({ ...form, prixVente: e.target.value })
                  }
                />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="quantiteStock">
                  Quantité en stock
                </FieldLabel>
                <Input
                  id="quantiteStock"
                  type="number"
                  min="0"
                  placeholder="0"
                  value={form.quantiteStock}
                  onChange={(e) =>
                    setForm({ ...form, quantiteStock: e.target.value })
                  }
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="seuilMinimum">Seuil minimum</FieldLabel>
                <Input
                  id="seuilMinimum"
                  type="number"
                  min="0"
                  placeholder="0"
                  value={form.seuilMinimum}
                  onChange={(e) =>
                    setForm({ ...form, seuilMinimum: e.target.value })
                  }
                />
              </Field>
            </div>
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
              form="create-product-form"
              loading={createProducts.isPending}
            >
              Créer le produit
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
