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
import { useUpdateCategory } from "@/hooks/use-categories";
import { getApiErrorMessage } from "@/lib/api-error";
import type { CategoryResponse } from "@/types/models";

interface EditCategoryDialogProps {
  category: CategoryResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const emptyForm = {
  name: "",
  description: "",
};

export function EditCategoryDialog({
  category,
  open,
  onOpenChange,
}: EditCategoryDialogProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loadedCategoryId, setLoadedCategoryId] = useState<number | null>(
    null,
  );
  const updateCategory = useUpdateCategory();

  if (category && loadedCategoryId !== category.id) {
    setLoadedCategoryId(category.id);
    setForm({
      name: category.name,
      description: category.description ?? "",
    });
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!category || !form.name.trim()) return;
    setError(null);

    try {
      await updateCategory.mutateAsync({
        id: category.id,
        data: {
          name: form.name.trim(),
          description: form.description.trim(),
        },
      });
      onOpenChange(false);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "Impossible de modifier la catégorie."),
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
          <DialogTitle>Modifier la catégorie</DialogTitle>
        </DialogHeader>

        <Form onSubmit={handleSubmit} id="edit-category-form">
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
                placeholder="Nom de la catégorie"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="edit-description">Description</FieldLabel>
              <Textarea
                id="edit-description"
                placeholder="Description de la catégorie"
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
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
              form="edit-category-form"
              loading={updateCategory.isPending}
            >
              Enregistrer
            </Button>
          </DialogFooter>
        </Form>
      </DialogPopup>
    </Dialog>
  );
}
