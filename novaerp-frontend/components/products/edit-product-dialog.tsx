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
import { useUpdateProduct } from "@/hooks/use-products";
import type { Product } from "@/types/models";

interface EditProductDialogProps {
  product: Product | null;
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

export function EditProductDialog({
  product,
  open,
  onOpenChange,
}: EditProductDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [loadedProductId, setLoadedProductId] = useState<number | null>(null);
  const updateProduct = useUpdateProduct();

  if (product && loadedProductId !== product.id) {
    setLoadedProductId(product.id);
    setForm({
      nom: product.nom,
      reference: product.reference,
      categorie: product.categorie ?? "",
      prixAchat: String(product.prixAchat),
      prixVente: String(product.prixVente),
      quantiteStock: String(product.quantiteStock),
      seuilMinimum: String(product.seuilMinimum),
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!product || !form.nom.trim() || !form.reference.trim()) return;

    await updateProduct.mutateAsync({
      id: product.id,
      data: {
        nom: form.nom.trim(),
        reference: form.reference.trim(),
        categorie: form.categorie.trim() || null,
        prixAchat: Number(form.prixAchat) || 0,
        prixVente: Number(form.prixVente) || 0,
        quantiteStock: Number(form.quantiteStock) || 0,
        seuilMinimum: Number(form.seuilMinimum) || 0,
      },
    });

    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-md">
        <DialogHeader>
          <DialogTitle>Modifier le produit</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-product-form">
          <DialogPanel className="flex flex-col gap-4">
            <Field>
              <FieldLabel htmlFor="edit-nom">Nom</FieldLabel>
              <Input
                id="edit-nom"
                required
                placeholder="Nom du produit"
                value={form.nom}
                onChange={(e) => setForm({ ...form, nom: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-reference">
                Référence (SKU)
              </FieldLabel>
              <Input
                id="edit-reference"
                required
                placeholder="REF-001"
                value={form.reference}
                onChange={(e) =>
                  setForm({ ...form, reference: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-categorie">Catégorie</FieldLabel>
              <Input
                id="edit-categorie"
                placeholder="Alimentation, Boissons..."
                value={form.categorie}
                onChange={(e) =>
                  setForm({ ...form, categorie: e.target.value })
                }
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-prixAchat">
                  Prix d&apos;achat
                </FieldLabel>
                <Input
                  id="edit-prixAchat"
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
                <FieldLabel htmlFor="edit-prixVente">
                  Prix de vente
                </FieldLabel>
                <Input
                  id="edit-prixVente"
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
                <FieldLabel htmlFor="edit-quantiteStock">
                  Quantité en stock
                </FieldLabel>
                <Input
                  id="edit-quantiteStock"
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
                <FieldLabel htmlFor="edit-seuilMinimum">
                  Seuil minimum
                </FieldLabel>
                <Input
                  id="edit-seuilMinimum"
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
              form="edit-product-form"
              loading={updateProduct.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
