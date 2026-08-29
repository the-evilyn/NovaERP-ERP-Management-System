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
import {
  Select,
  SelectItem,
  SelectPopup,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { useUpdateArticle } from "@/hooks/use-articles";
import { useCategories } from "@/hooks/use-categories";
import { useUnits } from "@/hooks/use-units";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ArticleResponse } from "@/types/models";

interface EditArticleDialogProps {
  article: ArticleResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  reference: "",
  designation: "",
  brand: "",
  barcode: "",
  categoryId: "",
  unitId: "",
  purchasePriceHt: "",
  unitCostTtc: "",
  salePriceHt: "",
  minStockQuantity: "",
  serialTracked: false,
  description: "",
  notes: "",
};

export function EditArticleDialog({
  article,
  open,
  onOpenChange,
}: EditArticleDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loadedArticleId, setLoadedArticleId] = useState<number | null>(null);
  const updateArticle = useUpdateArticle();
  const { data: categoriesPage } = useCategories(0, 100);
  const { data: unitsPage } = useUnits(0, 100);
  const categories = categoriesPage?.content;
  const units = unitsPage?.content;

  if (article && loadedArticleId !== article.id) {
    setLoadedArticleId(article.id);
    setForm({
      reference: article.reference,
      designation: article.designation,
      brand: article.brand ?? "",
      barcode: article.barcode ?? "",
      categoryId: article.categoryId ? String(article.categoryId) : "",
      unitId: article.unitId ? String(article.unitId) : "",
      purchasePriceHt: String(article.purchasePriceHt),
      unitCostTtc: String(article.unitCostTtc),
      salePriceHt: String(article.salePriceHt),
      minStockQuantity: String(article.minStockQuantity),
      serialTracked: article.serialTracked,
      description: article.description ?? "",
      notes: article.notes ?? "",
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!article || !form.reference.trim() || !form.designation.trim()) return;
    setError(null);

    try {
      await updateArticle.mutateAsync({
        id: article.id,
        data: {
          reference: form.reference.trim(),
          designation: form.designation.trim(),
          brand: form.brand.trim(),
          barcode: form.barcode.trim(),
          categoryId: form.categoryId ? Number(form.categoryId) : null,
          unitId: form.unitId ? Number(form.unitId) : null,
          purchasePriceHt: Number(form.purchasePriceHt) || 0,
          unitCostTtc: Number(form.unitCostTtc) || 0,
          salePriceHt: Number(form.salePriceHt) || 0,
          minStockQuantity: Number(form.minStockQuantity) || 0,
          serialTracked: form.serialTracked,
          description: form.description.trim(),
          notes: form.notes.trim(),
        },
      });
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de modifier l'article."));
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
      <DialogPopup className="max-w-xl">
        <DialogHeader>
          <DialogTitle>Modifier l'article</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-article-form">
          <DialogPanel className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-reference">Référence</FieldLabel>
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
                <FieldLabel htmlFor="edit-designation">
                  Désignation
                </FieldLabel>
                <Input
                  id="edit-designation"
                  required
                  placeholder="Nom de l'article"
                  value={form.designation}
                  onChange={(e) =>
                    setForm({ ...form, designation: e.target.value })
                  }
                />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-brand">Marque</FieldLabel>
                <Input
                  id="edit-brand"
                  placeholder="Marque"
                  value={form.brand}
                  onChange={(e) => setForm({ ...form, brand: e.target.value })}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="edit-barcode">Code-barres</FieldLabel>
                <Input
                  id="edit-barcode"
                  placeholder="Code-barres"
                  value={form.barcode}
                  onChange={(e) =>
                    setForm({ ...form, barcode: e.target.value })
                  }
                />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-categoryId">Catégorie</FieldLabel>
                <Select
                  items={(categories ?? []).map((c) => ({
                    label: c.name,
                    value: String(c.id),
                  }))}
                  value={form.categoryId || null}
                  onValueChange={(value) =>
                    setForm({ ...form, categoryId: value ?? "" })
                  }
                >
                  <SelectTrigger id="edit-categoryId">
                    <SelectValue placeholder="Sélectionner une catégorie" />
                  </SelectTrigger>
                  <SelectPopup>
                    {(categories ?? []).map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>
              <Field>
                <FieldLabel htmlFor="edit-unitId">Unité</FieldLabel>
                <Select
                  items={(units ?? []).map((u) => ({
                    label: `${u.name} (${u.symbol})`,
                    value: String(u.id),
                  }))}
                  value={form.unitId || null}
                  onValueChange={(value) =>
                    setForm({ ...form, unitId: value ?? "" })
                  }
                >
                  <SelectTrigger id="edit-unitId">
                    <SelectValue placeholder="Sélectionner une unité" />
                  </SelectTrigger>
                  <SelectPopup>
                    {(units ?? []).map((u) => (
                      <SelectItem key={u.id} value={String(u.id)}>
                        {u.name} ({u.symbol})
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-purchasePriceHt">
                  Prix d'achat HT
                </FieldLabel>
                <Input
                  id="edit-purchasePriceHt"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.purchasePriceHt}
                  onChange={(e) =>
                    setForm({ ...form, purchasePriceHt: e.target.value })
                  }
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="edit-unitCostTtc">
                  Coût unitaire TTC
                </FieldLabel>
                <Input
                  id="edit-unitCostTtc"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.unitCostTtc}
                  onChange={(e) =>
                    setForm({ ...form, unitCostTtc: e.target.value })
                  }
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="edit-salePriceHt">
                  Prix de vente HT
                </FieldLabel>
                <Input
                  id="edit-salePriceHt"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.salePriceHt}
                  onChange={(e) =>
                    setForm({ ...form, salePriceHt: e.target.value })
                  }
                />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="edit-minStockQuantity">
                  Stock minimum
                </FieldLabel>
                <Input
                  id="edit-minStockQuantity"
                  type="number"
                  min="0"
                  placeholder="0"
                  value={form.minStockQuantity}
                  onChange={(e) =>
                    setForm({ ...form, minStockQuantity: e.target.value })
                  }
                />
              </Field>
              <Field className="flex-row items-center justify-between pt-6">
                <FieldLabel htmlFor="edit-serialTracked">
                  Suivi par numéro de série
                </FieldLabel>
                <Switch
                  id="edit-serialTracked"
                  checked={form.serialTracked}
                  onCheckedChange={(checked) =>
                    setForm({ ...form, serialTracked: checked })
                  }
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="edit-description">Description</FieldLabel>
              <Textarea
                id="edit-description"
                placeholder="Description de l'article"
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-notes">Notes</FieldLabel>
              <Textarea
                id="edit-notes"
                placeholder="Notes internes"
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
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
              form="edit-article-form"
              loading={updateArticle.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
