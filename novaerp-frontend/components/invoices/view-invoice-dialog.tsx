"use client";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon, Cancel01Icon, CheckmarkCircle02Icon, Download01Icon, InboxDownloadIcon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogFooter, DialogHeader, DialogPanel, DialogPopup, DialogTitle } from "@/components/ui/dialog";
import { RecordPaymentDialog } from "@/components/payments/record-payment-dialog";
import { useCancelCustomerInvoice, useCancelSupplierInvoice, useIssueCustomerInvoice, useReceiveSupplierInvoice } from "@/hooks/use-invoices";
import { getApiErrorMessage } from "@/lib/api-error";
import { downloadPdfBlob } from "@/lib/pdf-download";
import { formatCurrency, formatDateTime, formatQuantity } from "@/lib/formatters";
import { useAuth } from "@/providers/auth-provider";
import { downloadCustomerInvoicePdf, downloadSupplierInvoicePdf } from "@/services/invoices.service";
import type { CustomerInvoiceResponse, SupplierInvoiceResponse } from "@/types/models";

type Props = { invoice: CustomerInvoiceResponse | SupplierInvoiceResponse | null; kind: "customer" | "supplier"; open: boolean; onOpenChange: (open: boolean) => void };
export function ViewInvoiceDialog({ invoice, kind, open, onOpenChange }: Props): React.ReactElement {
  const { user } = useAuth(); const [error, setError] = useState<string | null>(null); const [success, setSuccess] = useState<string | null>(null); const [paymentOpen, setPaymentOpen] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const issue = useIssueCustomerInvoice(); const receive = useReceiveSupplierInvoice(); const cancelCustomer = useCancelCustomerInvoice(); const cancelSupplier = useCancelSupplierInvoice();
  if (!invoice) return <></>;
  const isAdmin = user?.role === "ADMIN"; const partyName = kind === "customer" ? (invoice as CustomerInvoiceResponse).clientName : (invoice as SupplierInvoiceResponse).supplierName;
  const status = invoice.status;
  const badge = status === "DRAFT" ? <Badge variant="secondary">Brouillon</Badge> : status === "ISSUED" ? <Badge className="bg-blue-600 text-white">Émise</Badge> : status === "RECEIVED" ? <Badge className="bg-emerald-600 text-white">Reçue</Badge> : status === "PAID" ? <Badge className="bg-emerald-700 text-white">Payée</Badge> : <Badge variant="destructive">Annulée</Badge>;
  const run = async (action: () => Promise<unknown>, message: string) => { setError(null); setSuccess(null); try { await action(); setSuccess(message); } catch (reason) { setError(getApiErrorMessage(reason)); } };
  const busy = issue.isPending || receive.isPending || cancelCustomer.isPending || cancelSupplier.isPending || pdfLoading;

  const handleDownloadPdf = async () => {
    if (!invoice) return;
    setPdfLoading(true);
    setError(null);
    try {
      const { blob, filename } = kind === "customer"
        ? await downloadCustomerInvoicePdf(invoice.id)
        : await downloadSupplierInvoicePdf(invoice.id);
      downloadPdfBlob(blob, filename);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setPdfLoading(false);
    }
  };
  return <><Dialog open={open} onOpenChange={onOpenChange}><DialogPopup className="max-w-4xl max-h-[90vh] flex flex-col"><DialogHeader className="border-b pb-2"><DialogTitle className="flex items-center gap-2 font-mono">Facture : {invoice.invoiceNumber} {badge}</DialogTitle><p className="text-xs text-muted-foreground">Créée le {formatDateTime(invoice.createdAt)} {invoice.createdByName ? `par ${invoice.createdByName}` : ""}</p></DialogHeader><DialogPanel className="space-y-4 overflow-y-auto py-4">
    {error && <Alert variant="error"><HugeiconsIcon icon={AlertCircleIcon} className="size-4" /><AlertDescription>{error}</AlertDescription></Alert>}{success && <Alert><HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-4" /><AlertDescription>{success}</AlertDescription></Alert>}
    <div className="grid grid-cols-1 md:grid-cols-3 gap-3 rounded-lg border bg-muted/30 p-3 text-xs"><div><span className="block text-muted-foreground">{kind === "customer" ? "Client" : "Fournisseur"}</span><span className="font-semibold text-sm">{partyName}</span></div><div><span className="block text-muted-foreground">{kind === "customer" ? "Émise le" : "Reçue le"}</span><span>{kind === "customer" ? ((invoice as CustomerInvoiceResponse).issuedAt ? formatDateTime((invoice as CustomerInvoiceResponse).issuedAt!) : "Non émise") : ((invoice as SupplierInvoiceResponse).receivedAt ? formatDateTime((invoice as SupplierInvoiceResponse).receivedAt!) : "Non reçue")}</span></div><div><span className="block text-muted-foreground">Payée le</span><span>{invoice.paidAt ? formatDateTime(invoice.paidAt) : "Non payée"}</span></div></div>
    <div className="border rounded-md overflow-x-auto text-xs"><table className="w-full"><thead className="bg-muted/60"><tr><th className="p-2 text-left">Réf.</th><th className="p-2 text-left">Désignation</th><th className="p-2 text-right">Quantité</th><th className="p-2 text-right">Prix HT</th><th className="p-2 text-right">Total HT</th><th className="p-2 text-right">Total TTC</th></tr></thead><tbody>{invoice.items.map((item) => <tr key={item.id} className="border-t"><td className="p-2 font-mono">{item.articleReference}</td><td className="p-2">{item.articleDesignation}</td><td className="p-2 text-right">{formatQuantity(item.quantity)} {item.unitSymbol ?? ""}</td><td className="p-2 text-right">{formatCurrency(item.unitPrice)}</td><td className="p-2 text-right">{formatCurrency(item.totalHt)}</td><td className="p-2 text-right font-medium">{formatCurrency(item.totalTtc)}</td></tr>)}</tbody></table></div>
    <div className="ml-auto w-64 space-y-1 text-sm"><div className="flex justify-between"><span>Sous-total HT</span><span>{formatCurrency(invoice.subtotalHt)}</span></div><div className="flex justify-between"><span>TVA ({invoice.taxRate ?? 0}%)</span><span>{formatCurrency(invoice.taxAmount)}</span></div><div className="flex justify-between border-t pt-1 font-bold"><span>Total TTC</span><span>{formatCurrency(invoice.totalTtc)}</span></div></div>{invoice.notes && <div className="rounded border bg-muted/20 p-3 text-sm"><span className="font-medium">Notes : </span>{invoice.notes}</div>}
  </DialogPanel><DialogFooter className="flex flex-wrap justify-between gap-2"><div>{isAdmin && (status === "DRAFT" || status === "ISSUED" || status === "RECEIVED") && <Button variant="destructive" size="sm" disabled={busy} onClick={() => run(() => kind === "customer" ? cancelCustomer.mutateAsync(invoice.id) : cancelSupplier.mutateAsync(invoice.id), "Facture annulée.")}><HugeiconsIcon icon={Cancel01Icon} className="size-4" />Annuler</Button>}</div><div className="flex gap-2"><Button variant="outline" disabled={busy} onClick={handleDownloadPdf} className="gap-1.5"><HugeiconsIcon icon={Download01Icon} className="size-4" />{pdfLoading ? "Téléchargement..." : "Télécharger PDF"}</Button><Button variant="outline" onClick={() => onOpenChange(false)}>Fermer</Button>{isAdmin && status === "DRAFT" && <Button disabled={busy} onClick={() => run(() => kind === "customer" ? issue.mutateAsync(invoice.id) : receive.mutateAsync(invoice.id), kind === "customer" ? "Facture émise." : "Facture reçue.")}><HugeiconsIcon icon={kind === "customer" ? CheckmarkCircle02Icon : InboxDownloadIcon} className="size-4" />{kind === "customer" ? "Émettre" : "Réceptionner"}</Button>}{isAdmin && (status === "ISSUED" || status === "RECEIVED") && <Button disabled={busy} onClick={() => setPaymentOpen(true)}>Enregistrer un paiement</Button>}</div></DialogFooter></DialogPopup></Dialog><RecordPaymentDialog open={paymentOpen} onOpenChange={setPaymentOpen} type={kind} invoiceId={invoice.id} /></>;
}
