"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon, CheckmarkCircle01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useMemo, useState } from "react";
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
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  useCustomerInvoice,
  useCustomerInvoices,
  useSupplierInvoice,
  useSupplierInvoices,
} from "@/hooks/use-invoices";
import {
  useCustomerPaymentHistory,
  useRecordCustomerPayment,
  useRecordSupplierPayment,
  useSupplierPaymentHistory,
} from "@/hooks/use-payments";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatDate } from "@/lib/formatters";
import type { CustomerInvoiceResponse, PaymentMethod, SupplierInvoiceResponse } from "@/types/models";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  type: "customer" | "supplier";
  invoiceId?: number;
};

const methods: { value: PaymentMethod; label: string }[] = [
  { value: "BANK_TRANSFER", label: "Virement bancaire" },
  { value: "CASH", label: "Espèces" },
  { value: "CHECK", label: "Chèque" },
  { value: "CREDIT_CARD", label: "Carte bancaire" },
  { value: "OTHER", label: "Autre" },
];

function RecordPaymentForm({
  type,
  invoiceId,
  onClose,
}: {
  type: "customer" | "supplier";
  invoiceId?: number;
  onClose: () => void;
}): React.ReactElement {
  const [selectedId, setSelectedId] = useState<number | "">(invoiceId ?? "");
  const [customAmount, setCustomAmount] = useState<string | null>(null);
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER");
  const [paymentDate, setPaymentDate] = useState("");
  const [referenceNumber, setReferenceNumber] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);

  const activeInvoiceId = selectedId !== "" ? selectedId : (invoiceId ?? "");

  // Fetch list of eligible invoices from backend:
  // For customers: only ISSUED invoices can be paid
  // For suppliers: only RECEIVED invoices can be paid
  const customerInvoicesQuery = useCustomerInvoices(
    0,
    100,
    "ISSUED",
    undefined,
    undefined,
    { enabled: type === "customer" }
  );
  const supplierInvoicesQuery = useSupplierInvoices(
    0,
    100,
    "RECEIVED",
    undefined,
    undefined,
    { enabled: type === "supplier" }
  );

  const invoiceQuery = type === "customer" ? customerInvoicesQuery : supplierInvoicesQuery;

  // If a specific invoice is selected/passed, fetch its full detail to guarantee it is available
  const singleCustomerInvoice = useCustomerInvoice(
    type === "customer" && typeof activeInvoiceId === "number" ? activeInvoiceId : undefined
  );
  const singleSupplierInvoice = useSupplierInvoice(
    type === "supplier" && typeof activeInvoiceId === "number" ? activeInvoiceId : undefined
  );
  const singleInvoice = (type === "customer" ? singleCustomerInvoice.data : singleSupplierInvoice.data) as (CustomerInvoiceResponse | SupplierInvoiceResponse | undefined);

  // Payment summary for the active invoice
  const customerSummary = useCustomerPaymentHistory(
    type === "customer" && typeof activeInvoiceId === "number" ? activeInvoiceId : undefined
  );
  const supplierSummary = useSupplierPaymentHistory(
    type === "supplier" && typeof activeInvoiceId === "number" ? activeInvoiceId : undefined
  );
  const summary = type === "customer" ? customerSummary.data : supplierSummary.data;

  // Derive amount: if user hasn't typed anything, pre-fill with remaining balance once loaded
  const defaultRemaining = summary && typeof summary.remainingAmount === "number" && summary.remainingAmount > 0
    ? String(summary.remainingAmount)
    : "";
  const amount = customAmount !== null ? customAmount : defaultRemaining;

  // Mutations
  const customerMutation = useRecordCustomerPayment();
  const supplierMutation = useRecordSupplierPayment();
  const mutation = type === "customer" ? customerMutation : supplierMutation;

  // Filter and combine eligible invoices
  const invoices = useMemo(() => {
    const list = (invoiceQuery.data?.content ?? []).filter((inv) =>
      type === "customer" ? inv.status === "ISSUED" : inv.status === "RECEIVED"
    );
    if (singleInvoice) {
      const isEligible = type === "customer" ? singleInvoice.status === "ISSUED" : singleInvoice.status === "RECEIVED";
      if (isEligible && !list.some((inv) => inv.id === singleInvoice.id)) {
        return [singleInvoice, ...list];
      }
    }
    return list;
  }, [invoiceQuery.data?.content, singleInvoice, type]);

  const isFullyPaid = summary ? (summary.isFullyPaid || summary.remainingAmount <= 0) : false;
  const isInvoiceLoading = invoiceQuery.isLoading || (typeof activeInvoiceId === "number" && (singleCustomerInvoice.isLoading || singleSupplierInvoice.isLoading));
  const isSummaryLoading = typeof activeInvoiceId === "number" && (customerSummary.isLoading || supplierSummary.isLoading);

  // Retrieve party name and invoice date for active selection
  const selectedInvoice = useMemo(() => {
    if (!activeInvoiceId) return null;
    return invoices.find((inv) => inv.id === Number(activeInvoiceId)) ?? singleInvoice ?? null;
  }, [activeInvoiceId, invoices, singleInvoice]);

  const partyName = selectedInvoice
    ? type === "customer"
      ? (selectedInvoice as CustomerInvoiceResponse).clientName
      : (selectedInvoice as SupplierInvoiceResponse).supplierName
    : null;

  const invoiceDate = selectedInvoice?.createdAt ? formatDate(selectedInvoice.createdAt) : null;

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    const value = Number(amount);
    if (!activeInvoiceId) {
      return setError("Veuillez sélectionner une facture.");
    }
    if (!Number.isFinite(value) || value <= 0) {
      return setError("Le montant doit être strictement positif.");
    }
    if (summary && value > summary.remainingAmount) {
      return setError(
        `Le montant (${formatCurrency(value)}) dépasse le solde restant dû (${formatCurrency(summary.remainingAmount)}).`
      );
    }
    const data = {
      amount: value,
      paymentMethod: method,
      paymentDate: paymentDate ? new Date(paymentDate).toISOString() : undefined,
      referenceNumber: referenceNumber.trim() || undefined,
      notes: notes.trim() || undefined,
    };
    try {
      if (type === "customer") {
        await customerMutation.mutateAsync({ invoiceId: Number(activeInvoiceId), data });
      } else {
        await supplierMutation.mutateAsync({ invoiceId: Number(activeInvoiceId), data });
      }
      onClose();
    } catch (reason) {
      setError(getApiErrorMessage(reason));
    }
  };

  return (
    <form onSubmit={submit}>
      <DialogPanel className="space-y-4">
        {error && (
          <Alert variant="error">
            <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {!isInvoiceLoading && invoices.length === 0 && (
          <Alert variant="warning">
            <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
            <AlertDescription>
              {type === "customer"
                ? "Aucune facture client éligible au paiement trouvée. Seules les factures au statut « Émise » avec un solde restant dû peuvent recevoir un paiement."
                : "Aucune facture fournisseur éligible au paiement trouvée. Seules les factures au statut « Reçue » avec un solde restant dû peuvent recevoir un paiement."}
            </AlertDescription>
          </Alert>
        )}

        <Field>
          <FieldLabel>Facture *</FieldLabel>
          <select
            className="flex h-9 w-full rounded-md border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            value={activeInvoiceId || ""}
            disabled={isInvoiceLoading}
            onChange={(event) => {
              const newId = event.target.value ? Number(event.target.value) : "";
              setSelectedId(newId);
              setCustomAmount(null);
              setError(null);
            }}
            required
          >
            <option value="">
              {isInvoiceLoading
                ? "Chargement des factures éligibles..."
                : invoices.length === 0
                ? "-- Aucune facture éligible disponible --"
                : "-- Sélectionner une facture éligible --"}
            </option>
            {invoices.map((inv) => {
              const party =
                type === "customer"
                  ? (inv as CustomerInvoiceResponse).clientName
                  : (inv as SupplierInvoiceResponse).supplierName;
              return (
                <option key={inv.id} value={inv.id}>
                  {inv.invoiceNumber} {party ? `— ${party}` : ""} — {formatCurrency(inv.totalTtc)}
                </option>
              );
            })}
          </select>
        </Field>

        {Boolean(activeInvoiceId) && (
          <div className="space-y-2">
            <div className="rounded-lg border bg-muted/30 p-3 text-xs space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-1 border-b pb-2">
                <div>
                  <span className="text-muted-foreground">{type === "customer" ? "Client : " : "Fournisseur : "}</span>
                  <strong className="font-semibold">{partyName || "..."}</strong>
                </div>
                {invoiceDate && (
                  <div className="text-muted-foreground">
                    Date facture : <span>{invoiceDate}</span>
                  </div>
                )}
              </div>
              <div className="grid grid-cols-3 gap-2 pt-1 text-center sm:text-left">
                <div>
                  <span className="block text-muted-foreground">Total TTC</span>
                  <strong>{summary ? formatCurrency(summary.totalTtc) : isSummaryLoading ? "..." : "—"}</strong>
                </div>
                <div>
                  <span className="block text-muted-foreground">Déjà payé</span>
                  <strong>{summary ? formatCurrency(summary.totalPaid) : isSummaryLoading ? "..." : "—"}</strong>
                </div>
                <div>
                  <span className="block text-muted-foreground">Solde restant</span>
                  <strong className={isFullyPaid ? "text-emerald-600" : "text-primary"}>
                    {summary ? formatCurrency(summary.remainingAmount) : isSummaryLoading ? "..." : "—"}
                  </strong>
                </div>
              </div>
            </div>

            {isFullyPaid && (
              <Alert variant="info">
                <HugeiconsIcon icon={CheckmarkCircle01Icon} className="size-4 text-emerald-600" />
                <AlertDescription>Cette facture est déjà intégralement réglée.</AlertDescription>
              </Alert>
            )}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field>
            <div className="flex items-center justify-between">
              <FieldLabel>Montant *</FieldLabel>
              {summary && summary.remainingAmount > 0 && Number(amount) !== summary.remainingAmount && (
                <button
                  type="button"
                  className="text-xs text-primary hover:underline font-medium"
                  onClick={() => setCustomAmount(String(summary.remainingAmount))}
                >
                  Tout solder ({formatCurrency(summary.remainingAmount)})
                </button>
              )}
            </div>
            <Input
              type="number"
              min="0.0001"
              max={summary?.remainingAmount}
              step="0.0001"
              placeholder="0.00"
              value={amount}
              onChange={(event) => setCustomAmount(event.target.value)}
              disabled={isFullyPaid || !activeInvoiceId}
              required
            />
          </Field>

          <Field>
            <FieldLabel>Mode de paiement *</FieldLabel>
            <select
              className="flex h-9 w-full rounded-md border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              value={method}
              onChange={(event) => setMethod(event.target.value as PaymentMethod)}
              disabled={isFullyPaid || !activeInvoiceId}
              required
            >
              {methods.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </Field>

          <Field>
            <FieldLabel>Date de paiement</FieldLabel>
            <Input
              type="datetime-local"
              value={paymentDate}
              onChange={(event) => setPaymentDate(event.target.value)}
              disabled={isFullyPaid || !activeInvoiceId}
            />
          </Field>

          <Field>
            <FieldLabel>Référence</FieldLabel>
            <Input
              maxLength={100}
              placeholder="Ex: VIR-BMCE-2026-01"
              value={referenceNumber}
              onChange={(event) => setReferenceNumber(event.target.value)}
              disabled={isFullyPaid || !activeInvoiceId}
            />
          </Field>
        </div>

        <Field>
          <FieldLabel>Notes</FieldLabel>
          <Textarea
            maxLength={1000}
            placeholder="Commentaires optionnels..."
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            disabled={isFullyPaid || !activeInvoiceId}
          />
        </Field>
      </DialogPanel>

      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          Annuler
        </Button>
        <Button
          type="submit"
          disabled={
            mutation.isPending ||
            !activeInvoiceId ||
            isFullyPaid ||
            isSummaryLoading ||
            isInvoiceLoading
          }
        >
          {mutation.isPending ? "Enregistrement..." : "Enregistrer le paiement"}
        </Button>
      </DialogFooter>
    </form>
  );
}

export function RecordPaymentDialog({ open, onOpenChange, type, invoiceId }: Props): React.ReactElement {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            Enregistrer un paiement {type === "customer" ? "client" : "fournisseur"}
          </DialogTitle>
        </DialogHeader>
        {open && (
          <RecordPaymentForm
            key={`${type}-${invoiceId ?? "none"}`}
            type={type}
            invoiceId={invoiceId}
            onClose={() => onOpenChange(false)}
          />
        )}
      </DialogPopup>
    </Dialog>
  );
}
