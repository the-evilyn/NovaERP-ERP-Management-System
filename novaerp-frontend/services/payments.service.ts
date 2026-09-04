import { api } from "@/lib/axios";
import type { CustomerPaymentRequest, InvoicePaymentSummaryResponse, Page, PaymentResponse, PaymentType, SupplierPaymentRequest } from "@/types/models";

export async function getPayments(page = 0, size = 10, type?: PaymentType): Promise<Page<PaymentResponse>> {
  const { data } = await api.get<Page<PaymentResponse>>("/payments", { params: { page, size, type } });
  return data;
}
export async function getPayment(id: number): Promise<PaymentResponse> { const { data } = await api.get<PaymentResponse>(`/payments/${id}`); return data; }
export async function getCustomerInvoicePaymentSummary(invoiceId: number): Promise<InvoicePaymentSummaryResponse> { const { data } = await api.get<InvoicePaymentSummaryResponse>(`/payments/customer-invoices/${invoiceId}`); return data; }
export async function getSupplierInvoicePaymentSummary(invoiceId: number): Promise<InvoicePaymentSummaryResponse> { const { data } = await api.get<InvoicePaymentSummaryResponse>(`/payments/supplier-invoices/${invoiceId}`); return data; }
export async function recordCustomerPayment(invoiceId: number, payload: CustomerPaymentRequest): Promise<PaymentResponse> { const { data } = await api.post<PaymentResponse>(`/payments/customer-invoices/${invoiceId}`, payload); return data; }
export async function recordSupplierPayment(invoiceId: number, payload: SupplierPaymentRequest): Promise<PaymentResponse> { const { data } = await api.post<PaymentResponse>(`/payments/supplier-invoices/${invoiceId}`, payload); return data; }
export async function deletePayment(id: number): Promise<void> { await api.delete(`/payments/${id}`); }
