import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deletePayment, getCustomerInvoicePaymentSummary, getPayment, getPayments, getSupplierInvoicePaymentSummary, recordCustomerPayment, recordSupplierPayment } from "@/services/payments.service";
import type { CustomerPaymentRequest, PaymentType, SupplierPaymentRequest } from "@/types/models";

export function usePayments(page = 0, size = 10, type?: PaymentType) { return useQuery({ queryKey: ["payments", page, size, type], queryFn: () => getPayments(page, size, type) }); }
export function usePayment(id?: number) { return useQuery({ queryKey: ["payments", id], queryFn: () => getPayment(id as number), enabled: typeof id === "number" && Number.isFinite(id) }); }
export function useCustomerPaymentHistory(invoiceId?: number) { return useQuery({ queryKey: ["customer-payment-history", invoiceId], queryFn: () => getCustomerInvoicePaymentSummary(invoiceId as number), enabled: typeof invoiceId === "number" && Number.isFinite(invoiceId) }); }
export function useSupplierPaymentHistory(invoiceId?: number) { return useQuery({ queryKey: ["supplier-payment-history", invoiceId], queryFn: () => getSupplierInvoicePaymentSummary(invoiceId as number), enabled: typeof invoiceId === "number" && Number.isFinite(invoiceId) }); }
function usePaymentMutation<T>(mutationFn: (data: T) => Promise<unknown>) { const queryClient = useQueryClient(); return useMutation({ mutationFn, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["payments"] }); queryClient.invalidateQueries({ queryKey: ["customer-payment-history"] }); queryClient.invalidateQueries({ queryKey: ["supplier-payment-history"] }); queryClient.invalidateQueries({ queryKey: ["customer-invoices"] }); queryClient.invalidateQueries({ queryKey: ["supplier-invoices"] }); } }); }
export function useRecordCustomerPayment() { return usePaymentMutation(({ invoiceId, data }: { invoiceId: number; data: CustomerPaymentRequest }) => recordCustomerPayment(invoiceId, data)); }
export function useRecordSupplierPayment() { return usePaymentMutation(({ invoiceId, data }: { invoiceId: number; data: SupplierPaymentRequest }) => recordSupplierPayment(invoiceId, data)); }
export function useDeletePayment() { return usePaymentMutation((id: number) => deletePayment(id)); }
