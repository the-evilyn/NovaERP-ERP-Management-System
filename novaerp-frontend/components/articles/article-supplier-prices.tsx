"use client";

import { AlertCircleIcon, Delete02Icon, StarIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tooltip, TooltipTrigger, TooltipPopup } from "@/components/ui/tooltip";
import {
  useArticleSupplierPrices,
  useCreateArticleSupplierPrice,
  useDeleteArticleSupplierPrice,
  useSetPrimarySupplierPrice,
} from "@/hooks/use-articles";
import { useSuppliers } from "@/hooks/use-suppliers";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ArticleResponse } from "@/types/models";

const currency = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 2,
});

const emptyForm = {
  supplierId: "",
  primary: false,
  priceHt: "",
  priceTtc: "",
  taxRate: "",
  leadTimeDays: "",
  quoteDate: "",
};

export function ArticleSupplierPrices({
  article,
}: {
  article: ArticleResponse;
}): React.ReactElement {
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const { data: prices, isPending } = useArticleSupplierPrices(article.id);
  const { data: suppliersPage } = useSuppliers(0, 100);
  const suppliers = suppliersPage?.content;
  const createPrice = useCreateArticleSupplierPrice(article.id);
  const deletePrice = useDeleteArticleSupplierPrice(article.id);
  const setPrimary = useSetPrimarySupplierPrice(article.id);

  const sortedPrices = [...(prices ?? [])].sort(
    (a, b) => Number(b.primary) - Number(a.primary),
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.supplierId || !form.priceHt || !form.priceTtc) return;
    setError(null);

    try {
      await createPrice.mutateAsync({
        supplierId: Number(form.supplierId),
        primary: form.primary,
        priceHt: Number(form.priceHt) || 0,
        priceTtc: Number(form.priceTtc) || 0,
        taxRate: form.taxRate ? Number(form.taxRate) : 0,
        leadTimeDays: form.leadTimeDays ? Number(form.leadTimeDays) : null,
        quoteDate: form.quoteDate || null,
      });
      setForm(emptyForm);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter le tarif."));
    }
  };

  const handleDelete = async (priceId: number) => {
    setError(null);
    try {
      await deletePrice.mutateAsync(priceId);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de supprimer le tarif."));
    }
  };

  const handleSetPrimary = async (priceId: number) => {
    setError(null);
    try {
      await setPrimary.mutateAsync(priceId);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de définir le fournisseur principal."));
    }
  };

  return (
    <div className="flex flex-col gap-4 p-4">
      <div>
        <h3 className="font-semibold text-sm">Tarifs fournisseurs</h3>
        <p className="text-muted-foreground text-xs">
          Devis de prix reçus des fournisseurs pour {article.designation}.
        </p>
      </div>

      {error && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-10" />
            <TableHead>Fournisseur</TableHead>
            <TableHead>Prix HT</TableHead>
            <TableHead>Prix TTC</TableHead>
            <TableHead>Délai</TableHead>
            <TableHead>Date du devis</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isPending && (
            <TableRow>
              <TableCell colSpan={7} className="text-center text-muted-foreground">
                Chargement...
              </TableCell>
            </TableRow>
          )}
          {!isPending && sortedPrices.length === 0 && (
            <TableRow>
              <TableCell colSpan={7} className="text-center text-muted-foreground">
                Aucun tarif fournisseur enregistré.
              </TableCell>
            </TableRow>
          )}
          {sortedPrices.map((price) => (
            <TableRow key={price.id}>
              <TableCell>
                {price.primary ? (
                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <Badge variant="warning" size="sm">
                          <HugeiconsIcon icon={StarIcon} strokeWidth={2} />
                        </Badge>
                      }
                    />
                    <TooltipPopup>Fournisseur principal</TooltipPopup>
                  </Tooltip>
                ) : (
                  <Tooltip>
                    <TooltipTrigger
                      render={
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => handleSetPrimary(price.id)}
                          loading={setPrimary.isPending}
                        >
                          <HugeiconsIcon icon={StarIcon} strokeWidth={2} />
                        </Button>
                      }
                    />
                    <TooltipPopup>Définir comme principal</TooltipPopup>
                  </Tooltip>
                )}
              </TableCell>
              <TableCell className="font-medium">
                {price.supplierName}
              </TableCell>
              <TableCell>{currency.format(price.priceHt)}</TableCell>
              <TableCell>{currency.format(price.priceTtc)}</TableCell>
              <TableCell className="text-muted-foreground">
                {price.leadTimeDays != null
                  ? `${price.leadTimeDays} j`
                  : "—"}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {price.quoteDate ?? "—"}
              </TableCell>
              <TableCell className="text-right">
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => handleDelete(price.id)}
                  loading={deletePrice.isPending}
                >
                  <HugeiconsIcon icon={Delete02Icon} strokeWidth={2} />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Form onSubmit={handleSubmit} id={`add-supplier-price-form-${article.id}`}>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-6">
          <Field>
            <FieldLabel htmlFor={`supplierId-${article.id}`}>
              Fournisseur
            </FieldLabel>
            <Select
              items={(suppliers ?? []).map((s) => ({
                label: s.name,
                value: String(s.id),
              }))}
              value={form.supplierId || null}
              onValueChange={(value) =>
                setForm({ ...form, supplierId: value ?? "" })
              }
            >
              <SelectTrigger id={`supplierId-${article.id}`} size="sm">
                <SelectValue placeholder="Fournisseur" />
              </SelectTrigger>
              <SelectPopup>
                {(suppliers ?? []).map((s) => (
                  <SelectItem key={s.id} value={String(s.id)}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectPopup>
            </Select>
          </Field>
          <Field className="flex-row items-center justify-between pt-6">
            <FieldLabel htmlFor={`primary-${article.id}`}>
              Fournisseur principal
            </FieldLabel>
            <Switch
              id={`primary-${article.id}`}
              checked={form.primary}
              onCheckedChange={(checked) =>
                setForm({ ...form, primary: checked })
              }
            />
          </Field>
          <Field>
            <FieldLabel htmlFor={`priceHt-${article.id}`}>Prix HT</FieldLabel>
            <Input
              id={`priceHt-${article.id}`}
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.priceHt}
              onChange={(e) => setForm({ ...form, priceHt: e.target.value })}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor={`priceTtc-${article.id}`}>
              Prix TTC
            </FieldLabel>
            <Input
              id={`priceTtc-${article.id}`}
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={form.priceTtc}
              onChange={(e) => setForm({ ...form, priceTtc: e.target.value })}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor={`leadTimeDays-${article.id}`}>
              Délai (j)
            </FieldLabel>
            <Input
              id={`leadTimeDays-${article.id}`}
              type="number"
              min="0"
              placeholder="0"
              value={form.leadTimeDays}
              onChange={(e) =>
                setForm({ ...form, leadTimeDays: e.target.value })
              }
            />
          </Field>
          <Field>
            <FieldLabel htmlFor={`quoteDate-${article.id}`}>
              Date du devis
            </FieldLabel>
            <Input
              id={`quoteDate-${article.id}`}
              type="date"
              value={form.quoteDate}
              onChange={(e) =>
                setForm({ ...form, quoteDate: e.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-3 flex justify-end">
          <Button
            type="submit"
            size="sm"
            variant="outline"
            loading={createPrice.isPending}
          >
            Ajouter le tarif
          </Button>
        </div>
      </Form>
    </div>
  );
}
