"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, AlertCircleIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogFooter, DialogHeader, DialogPanel, DialogPopup, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useArticles } from "@/hooks/use-articles";
import { useClients } from "@/hooks/use-clients";
import { useCreateCustomerInvoice, useCreateSupplierInvoice } from "@/hooks/use-invoices";
import { useSuppliers } from "@/hooks/use-suppliers";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";

export type InvoiceParty = "customer" | "supplier";
interface Props { open: boolean; onOpenChange: (open: boolean) => void; party: InvoiceParty; }
interface Line { articleId: number; quantity: number; unitPrice: number; taxRate?: number; }

export function CreateInvoiceDialog({ open, onOpenChange, party }: Props): React.ReactElement {
  const [partyId, setPartyId] = useState<number | "">("");
  const [taxRate, setTaxRate] = useState(20);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [lines, setLines] = useState<Line[]>([{ articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 }]);
  const { data: clientsData, isLoading: loadingClients } = useClients(0, 100);
  const { data: suppliersData, isLoading: loadingSuppliers } = useSuppliers(0, 100);
  const { data: articlesData, isLoading: loadingArticles } = useArticles(0, 300);
  const customerMutation = useCreateCustomerInvoice();
  const supplierMutation = useCreateSupplierInvoice();
  const parties = party === "customer" ? clientsData?.content ?? [] : suppliersData?.content ?? [];
  const isLoadingParties = party === "customer" ? loadingClients : loadingSuppliers;
  const mutation = party === "customer" ? customerMutation : supplierMutation;
  const articles = articlesData?.content ?? [];
  const label = party === "customer" ? "Client" : "Fournisseur";
  const defaultPrice = party === "customer" ? "salePriceHt" : "purchasePriceHt";
  const subtotal = lines.reduce((sum, line) => sum + (line.articleId > 0 ? line.quantity * line.unitPrice : 0), 0);
  const tax = lines.reduce((sum, line) => sum + (line.articleId > 0 ? line.quantity * line.unitPrice * (line.taxRate ?? taxRate) / 100 : 0), 0);
  const update = (index: number, patch: Partial<Line>) => setLines((current) => current.map((line, i) => i === index ? { ...line, ...patch } : line));
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(null);
    const items = lines.filter((line) => line.articleId > 0 && line.quantity > 0);
    if (!partyId) return setError(`Veuillez sélectionner un ${label.toLowerCase()}.`);
    if (!items.length) return setError("Veuillez ajouter au moins un article valide.");
    try {
      if (party === "customer") await customerMutation.mutateAsync({ clientId: Number(partyId), items, taxRate, notes: notes.trim() || undefined });
      else await supplierMutation.mutateAsync({ supplierId: Number(partyId), items, taxRate, notes: notes.trim() || undefined });
      setPartyId(""); setNotes(""); setLines([{ articleId: 0, quantity: 1, unitPrice: 0, taxRate: 20 }]); onOpenChange(false);
    } catch (reason) { setError(getApiErrorMessage(reason)); }
  };
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogPopup className="max-w-4xl max-h-[90vh] overflow-y-auto"><DialogHeader><DialogTitle>Nouvelle facture {party === "customer" ? "client" : "fournisseur"}</DialogTitle></DialogHeader><form onSubmit={submit}><DialogPanel className="space-y-4">
    {error && <Alert variant="error"><HugeiconsIcon icon={AlertCircleIcon} className="size-4" /><AlertDescription>{error}</AlertDescription></Alert>}
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4"><Field className="md:col-span-2"><FieldLabel>{label} *</FieldLabel><select className="flex h-9 w-full rounded-md border border-input bg-background px-3 text-sm" value={partyId} onChange={(e) => setPartyId(e.target.value ? Number(e.target.value) : "")} disabled={isLoadingParties} required><option value="">{isLoadingParties ? "Chargement..." : `-- Sélectionner un ${label.toLowerCase()} --`}</option>{parties.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></Field><Field><FieldLabel>Taux TVA (%)</FieldLabel><Input type="number" min="0" step="0.5" value={taxRate} onChange={(e) => setTaxRate(Number(e.target.value))} /></Field></div>
    <div className="space-y-2"><div className="flex justify-between items-center"><h4 className="text-sm font-medium">Lignes de facture</h4><Button type="button" variant="outline" size="sm" className="gap-1" onClick={() => setLines((current) => [...current, { articleId: 0, quantity: 1, unitPrice: 0, taxRate }])}><HugeiconsIcon icon={Add01Icon} className="size-4" />Ajouter un article</Button></div><div className="border rounded-lg overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/50 text-xs"><tr><th className="p-2 text-left">Article</th><th className="p-2">Qté</th><th className="p-2">Prix HT</th><th className="p-2 text-right">Total HT</th><th /></tr></thead><tbody>{lines.map((line, index) => <tr key={index} className="border-t"><td className="p-2"><select className="h-8 w-full rounded border border-input bg-background px-2 text-xs" value={line.articleId || ""} disabled={loadingArticles} onChange={(e) => { const article = articles.find((a) => a.id === Number(e.target.value)); update(index, { articleId: Number(e.target.value), unitPrice: article?.[defaultPrice] ?? 0 }); }} required><option value="">-- Sélectionner un article --</option>{articles.map((article) => <option key={article.id} value={article.id}>{article.reference} - {article.designation}</option>)}</select></td><td className="p-2 w-24"><Input className="h-8" type="number" min="0.0001" step="any" value={line.quantity} onChange={(e) => update(index, { quantity: Number(e.target.value) })} required /></td><td className="p-2 w-32"><Input className="h-8" type="number" min="0" step="0.01" value={line.unitPrice} onChange={(e) => update(index, { unitPrice: Number(e.target.value) })} required /></td><td className="p-2 text-right font-medium">{formatCurrency(line.quantity * line.unitPrice)}</td><td className="p-2"><Button type="button" variant="ghost" size="sm" disabled={lines.length === 1} onClick={() => setLines((current) => current.filter((_, i) => i !== index))}><HugeiconsIcon icon={Delete02Icon} className="size-4" /></Button></td></tr>)}</tbody></table></div></div>
    <Field><FieldLabel>Notes</FieldLabel><Textarea value={notes} onChange={(e) => setNotes(e.target.value)} /></Field><div className="ml-auto w-64 space-y-1 text-sm"><div className="flex justify-between"><span>Sous-total HT</span><span>{formatCurrency(subtotal)}</span></div><div className="flex justify-between"><span>TVA</span><span>{formatCurrency(tax)}</span></div><div className="flex justify-between border-t pt-1 font-bold"><span>Total TTC</span><span>{formatCurrency(subtotal + tax)}</span></div></div>
  </DialogPanel><DialogFooter><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Annuler</Button><Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Création..." : "Créer la facture"}</Button></DialogFooter></form></DialogPopup></Dialog>;
}
