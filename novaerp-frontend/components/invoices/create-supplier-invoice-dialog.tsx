"use client";
import type React from "react";
import { CreateInvoiceDialog } from "@/components/invoices/create-invoice-dialog";
export function CreateSupplierInvoiceDialog(props: { open: boolean; onOpenChange: (open: boolean) => void }): React.ReactElement { return <CreateInvoiceDialog {...props} party="supplier" />; }
