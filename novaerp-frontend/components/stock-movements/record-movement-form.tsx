"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
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
import { useCreateStockMovement } from "@/hooks/use-stock-movements";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ArticleResponse, StockMovementType } from "@/types/models";

interface RecordMovementFormProps {
  article: ArticleResponse;
}

const movementTypeOptions: { label: string; value: StockMovementType }[] = [
  { label: "Entrée", value: "IN" },
  { label: "Sortie", value: "OUT" },
  { label: "Ajustement", value: "ADJUSTMENT" },
];

const emptyForm = {
  type: "IN" as StockMovementType,
  quantity: "",
  reference: "",
  note: "",
};

export function RecordMovementForm({
  article,
}: RecordMovementFormProps): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const createMovement = useCreateStockMovement();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const quantity = Number(form.quantity);
    if (!quantity || quantity <= 0) return;
    setError(null);

    try {
      await createMovement.mutateAsync({
        articleId: article.id,
        type: form.type,
        quantity,
        reference: form.reference.trim(),
        note: form.note.trim(),
      });
      setForm(emptyForm);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "Impossible d'enregistrer le mouvement."),
      );
    }
  };

  return (
    <Card>
      <CardHeader>
        <h2 className="font-semibold text-sm">Nouveau mouvement</h2>
        <p className="text-muted-foreground text-xs">
          Stock actuel : {article.stockQuantity} {article.unitName ?? "u."}
        </p>
      </CardHeader>
      <CardContent>
        <Form onSubmit={handleSubmit} id="record-movement-form">
          <div className="flex flex-col gap-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <div className="grid grid-cols-2 gap-4">
              <Field>
                <FieldLabel htmlFor="movement-type">Type</FieldLabel>
                <Select
                  items={movementTypeOptions}
                  value={form.type}
                  onValueChange={(value) =>
                    setForm({
                      ...form,
                      type: (value as StockMovementType) ?? "IN",
                    })
                  }
                >
                  <SelectTrigger id="movement-type">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectPopup>
                    {movementTypeOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectPopup>
                </Select>
              </Field>
              <Field>
                <FieldLabel htmlFor="movement-quantity">Quantité</FieldLabel>
                <Input
                  id="movement-quantity"
                  type="number"
                  min="1"
                  required
                  placeholder="0"
                  value={form.quantity}
                  onChange={(e) =>
                    setForm({ ...form, quantity: e.target.value })
                  }
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="movement-reference">Référence</FieldLabel>
              <Input
                id="movement-reference"
                placeholder="Bon de commande, facture..."
                value={form.reference}
                onChange={(e) =>
                  setForm({ ...form, reference: e.target.value })
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="movement-note">Note</FieldLabel>
              <Input
                id="movement-note"
                placeholder="Note optionnelle"
                value={form.note}
                onChange={(e) => setForm({ ...form, note: e.target.value })}
              />
            </Field>
            <Button
              type="submit"
              form="record-movement-form"
              loading={createMovement.isPending}
              className="self-start"
            >
              Enregistrer le mouvement
            </Button>
          </div>
        </Form>
      </CardContent>
    </Card>
  );
}
