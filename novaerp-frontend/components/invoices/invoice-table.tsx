"use client";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon, Download01Icon, EyeIcon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ViewInvoiceDialog } from "@/components/invoices/view-invoice-dialog";
import { useCustomerInvoices, useSupplierInvoices } from "@/hooks/use-invoices";
import { downloadPdfBlob } from "@/lib/pdf-download";
import { formatCurrency, formatDate } from "@/lib/formatters";
import { downloadCustomerInvoicePdf, downloadSupplierInvoicePdf } from "@/services/invoices.service";
import type { CustomerInvoiceResponse, SupplierInvoiceResponse } from "@/types/models";

export function InvoiceTable({ kind }: { kind: "customer" | "supplier" }): React.ReactElement {
  const [page, setPage] = useState(0); const [selected, setSelected] = useState<CustomerInvoiceResponse | SupplierInvoiceResponse | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const customer = useCustomerInvoices(page, 10); const supplier = useSupplierInvoices(page, 10); const query = kind === "customer" ? customer : supplier;
  const rows = query.data?.content ?? []; const partyLabel = kind === "customer" ? "Client" : "Fournisseur";
  const statusBadge = (status: string) => status === "DRAFT" ? <Badge variant="secondary">Brouillon</Badge> : status === "PAID" ? <Badge className="bg-emerald-700 text-white">Payée</Badge> : status === "CANCELLED" ? <Badge variant="destructive">Annulée</Badge> : <Badge className="bg-blue-600 text-white">{status === "ISSUED" ? "Émise" : "Reçue"}</Badge>;

  const handleDownloadPdf = async (e: React.MouseEvent, inv: CustomerInvoiceResponse | SupplierInvoiceResponse) => {
    e.stopPropagation();
    setDownloadingId(inv.id);
    try {
      const { blob, filename } = kind === "customer"
        ? await downloadCustomerInvoicePdf(inv.id)
        : await downloadSupplierInvoicePdf(inv.id);
      downloadPdfBlob(blob, filename);
    } catch {
      // Error handled gracefully
    } finally {
      setDownloadingId(null);
    }
  };

  return <div className="space-y-3"><div className="border rounded-lg bg-card overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/50 text-xs text-muted-foreground"><tr><th className="p-3 text-left">N° Facture</th><th className="p-3 text-left">{partyLabel}</th><th className="p-3 text-left">Date</th><th className="p-3 text-center">Statut</th><th className="p-3 text-right">Total TTC</th><th className="p-3 text-right">Actions</th></tr></thead><tbody className="divide-y text-xs">{query.isLoading ? <tr><td colSpan={6} className="p-8 text-center text-muted-foreground">Chargement des factures...</td></tr> : query.isError ? <tr><td colSpan={6} className="p-8 text-center text-destructive"><HugeiconsIcon icon={AlertCircleIcon} className="inline size-4 mr-1" />Impossible de récupérer les factures.</td></tr> : rows.length === 0 ? <tr><td colSpan={6} className="p-10 text-center text-muted-foreground">Aucune facture {kind === "customer" ? "client" : "fournisseur"}.</td></tr> : rows.map((invoice) => { const name = kind === "customer" ? (invoice as CustomerInvoiceResponse).clientName : (invoice as SupplierInvoiceResponse).supplierName; return <tr key={invoice.id} className="cursor-pointer hover:bg-muted/20" onClick={() => setSelected(invoice)}><td className="p-3 font-semibold text-primary">{invoice.invoiceNumber}</td><td className="p-3 font-medium">{name}</td><td className="p-3 text-muted-foreground">{formatDate(invoice.createdAt)}</td><td className="p-3 text-center">{statusBadge(invoice.status)}</td><td className="p-3 text-right font-bold">{formatCurrency(invoice.totalTtc)}</td><td className="p-3 text-right"><div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}><Button variant="ghost" size="sm" className="h-8 px-2" disabled={downloadingId === invoice.id} onClick={(e) => handleDownloadPdf(e, invoice)} title="Télécharger PDF"><HugeiconsIcon icon={Download01Icon} className="size-4" /></Button><Button variant="ghost" size="sm" className="h-8 gap-1" onClick={() => setSelected(invoice)}><HugeiconsIcon icon={EyeIcon} className="size-4" />Détails</Button></div></td></tr>; })}</tbody></table></div>{(query.data?.totalPages ?? 0) > 1 && <div className="flex justify-between"><Button size="sm" variant="outline" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Précédent</Button><Button size="sm" variant="outline" disabled={page >= (query.data?.totalPages ?? 1) - 1} onClick={() => setPage((value) => value + 1)}>Suivant</Button></div>}<ViewInvoiceDialog invoice={selected} kind={kind} open={selected !== null} onOpenChange={(value) => !value && setSelected(null)} /></div>;
}
