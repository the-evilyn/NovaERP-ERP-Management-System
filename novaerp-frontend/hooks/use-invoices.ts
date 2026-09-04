import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelCustomerInvoice, cancelSupplierInvoice, createCustomerInvoice, createSupplierInvoice, getCustomerInvoice, getCustomerInvoices, getSupplierInvoice, getSupplierInvoices, issueCustomerInvoice, receiveSupplierInvoice } from "@/services/invoices.service";
import type { CustomerInvoiceRequest, CustomerInvoiceStatus, SupplierInvoiceRequest, SupplierInvoiceStatus } from "@/types/models";

export function useCustomerInvoices(page = 0, size = 10, status?: CustomerInvoiceStatus, clientId?: number, saleOrderId?: number) { return useQuery({ queryKey: ["customer-invoices", page, size, status, clientId, saleOrderId], queryFn: () => getCustomerInvoices(page, size, status, clientId, saleOrderId) }); }
export function useSupplierInvoices(page = 0, size = 10, status?: SupplierInvoiceStatus, supplierId?: number, purchaseOrderId?: number) { return useQuery({ queryKey: ["supplier-invoices", page, size, status, supplierId, purchaseOrderId], queryFn: () => getSupplierInvoices(page, size, status, supplierId, purchaseOrderId) }); }
export function useCustomerInvoice(id?: number) { return useQuery({ queryKey: ["customer-invoices", id], queryFn: () => getCustomerInvoice(id as number), enabled: typeof id === "number" && Number.isFinite(id) }); }
export function useSupplierInvoice(id?: number) { return useQuery({ queryKey: ["supplier-invoices", id], queryFn: () => getSupplierInvoice(id as number), enabled: typeof id === "number" && Number.isFinite(id) }); }
function useInvoiceMutation<T>(mutationFn: (data: T) => Promise<unknown>) { const client = useQueryClient(); return useMutation({ mutationFn, onSuccess: () => { client.invalidateQueries({ queryKey: ["customer-invoices"] }); client.invalidateQueries({ queryKey: ["supplier-invoices"] }); } }); }
export function useCreateCustomerInvoice() { return useInvoiceMutation((data: CustomerInvoiceRequest) => createCustomerInvoice(data)); }
export function useCreateSupplierInvoice() { return useInvoiceMutation((data: SupplierInvoiceRequest) => createSupplierInvoice(data)); }
export function useIssueCustomerInvoice() { return useInvoiceMutation(issueCustomerInvoice); }
export function useCancelCustomerInvoice() { return useInvoiceMutation(cancelCustomerInvoice); }
export function useReceiveSupplierInvoice() { return useInvoiceMutation(receiveSupplierInvoice); }
export function useCancelSupplierInvoice() { return useInvoiceMutation(cancelSupplierInvoice); }
