"use client";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { CreateCustomerInvoiceDialog } from "@/components/invoices/create-customer-invoice-dialog";
import { CreateSupplierInvoiceDialog } from "@/components/invoices/create-supplier-invoice-dialog";
import { CustomerInvoiceTable } from "@/components/invoices/customer-invoice-table";
import { SupplierInvoiceTable } from "@/components/invoices/supplier-invoice-table";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/providers/auth-provider";

export default function InvoicesPage(): React.ReactElement {
  const { user } = useAuth();
  const [customerOpen, setCustomerOpen] = useState(false);
  const [supplierOpen, setSupplierOpen] = useState(false);
  const isAdmin = user?.role === "ADMIN";

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Factures</h1>
          <p className="text-sm text-muted-foreground">
            Suivez les factures clients et fournisseurs depuis leur création jusqu&apos;à leur règlement.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {isAdmin && (
            <Button variant="outline" onClick={() => setSupplierOpen(true)}>
              <HugeiconsIcon icon={Add01Icon} className="size-4" />
              Facture fournisseur
            </Button>
          )}
          <Button onClick={() => setCustomerOpen(true)}>
            <HugeiconsIcon icon={Add01Icon} className="size-4" />
            Facture client
          </Button>
        </div>
      </div>
      <Tabs defaultValue="customers">
        <TabsList>
          <TabsTrigger value="customers">Factures clients</TabsTrigger>
          <TabsTrigger value="suppliers">Factures fournisseurs</TabsTrigger>
        </TabsList>
        <TabsContent value="customers" className="pt-4">
          <CustomerInvoiceTable />
        </TabsContent>
        <TabsContent value="suppliers" className="pt-4">
          <SupplierInvoiceTable />
        </TabsContent>
      </Tabs>
      <CreateCustomerInvoiceDialog open={customerOpen} onOpenChange={setCustomerOpen} />
      <CreateSupplierInvoiceDialog open={supplierOpen} onOpenChange={setSupplierOpen} />
    </div>
  );
}
