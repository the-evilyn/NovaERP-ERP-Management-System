"use client";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { RecordPaymentDialog } from "@/components/payments/record-payment-dialog";
import { PaymentHistoryTable } from "@/components/payments/payment-history-table";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/providers/auth-provider";

export default function PaymentsPage(): React.ReactElement {
  const { user } = useAuth(); const [customerOpen, setCustomerOpen] = useState(false); const [supplierOpen, setSupplierOpen] = useState(false);
  return <div className="flex flex-col gap-6"><div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><div><h1 className="text-2xl font-bold tracking-tight">Paiements</h1><p className="text-sm text-muted-foreground">Enregistrez les règlements clients et fournisseurs et consultez leur historique.</p></div>{user?.role === "ADMIN" && <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => setSupplierOpen(true)}><HugeiconsIcon icon={Add01Icon} className="size-4" />Paiement fournisseur</Button><Button onClick={() => setCustomerOpen(true)}><HugeiconsIcon icon={Add01Icon} className="size-4" />Paiement client</Button></div>}</div><Tabs defaultValue="customers"><TabsList><TabsTrigger value="customers">Paiements clients</TabsTrigger><TabsTrigger value="suppliers">Paiements fournisseurs</TabsTrigger></TabsList><TabsContent value="customers" className="pt-4"><PaymentHistoryTable type="CUSTOMER_PAYMENT" /></TabsContent><TabsContent value="suppliers" className="pt-4"><PaymentHistoryTable type="SUPPLIER_PAYMENT" /></TabsContent></Tabs><RecordPaymentDialog open={customerOpen} onOpenChange={setCustomerOpen} type="customer" /><RecordPaymentDialog open={supplierOpen} onOpenChange={setSupplierOpen} type="supplier" /></div>;
}
