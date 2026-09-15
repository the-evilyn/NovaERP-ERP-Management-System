import { api } from "@/lib/axios";
import type { CustomerInvoiceRequest, CustomerInvoiceResponse, CustomerInvoiceStatus, Page, SupplierInvoiceRequest, SupplierInvoiceResponse, SupplierInvoiceStatus } from "@/types/models";

export async function getCustomerInvoices(page = 0, size = 10, status?: CustomerInvoiceStatus, clientId?: number, saleOrderId?: number): Promise<Page<CustomerInvoiceResponse>> {
  const { data } = await api.get<Page<CustomerInvoiceResponse>>("/invoices/customers", { params: { page, size, status, clientId, saleOrderId } });
  return data;
}
export async function getCustomerInvoice(id: number): Promise<CustomerInvoiceResponse> { const { data } = await api.get<CustomerInvoiceResponse>(`/invoices/customers/${id}`); return data; }
export async function createCustomerInvoice(payload: CustomerInvoiceRequest): Promise<CustomerInvoiceResponse> { const { data } = await api.post<CustomerInvoiceResponse>("/invoices/customers", payload); return data; }
export async function createCustomerInvoiceFromSaleOrder(saleOrderId: number): Promise<CustomerInvoiceResponse> { const { data } = await api.post<CustomerInvoiceResponse>(`/invoices/customers/from-sale-order/${saleOrderId}`); return data; }
export async function issueCustomerInvoice(id: number): Promise<CustomerInvoiceResponse> { const { data } = await api.post<CustomerInvoiceResponse>(`/invoices/customers/${id}/issue`); return data; }
export async function cancelCustomerInvoice(id: number): Promise<CustomerInvoiceResponse> { const { data } = await api.post<CustomerInvoiceResponse>(`/invoices/customers/${id}/cancel`); return data; }

export async function getSupplierInvoices(page = 0, size = 10, status?: SupplierInvoiceStatus, supplierId?: number, purchaseOrderId?: number): Promise<Page<SupplierInvoiceResponse>> {
  const { data } = await api.get<Page<SupplierInvoiceResponse>>("/invoices/suppliers", { params: { page, size, status, supplierId, purchaseOrderId } });
  return data;
}
export async function getSupplierInvoice(id: number): Promise<SupplierInvoiceResponse> { const { data } = await api.get<SupplierInvoiceResponse>(`/invoices/suppliers/${id}`); return data; }
export async function createSupplierInvoice(payload: SupplierInvoiceRequest): Promise<SupplierInvoiceResponse> { const { data } = await api.post<SupplierInvoiceResponse>("/invoices/suppliers", payload); return data; }
export async function createSupplierInvoiceFromPurchaseOrder(purchaseOrderId: number): Promise<SupplierInvoiceResponse> { const { data } = await api.post<SupplierInvoiceResponse>(`/invoices/suppliers/from-purchase-order/${purchaseOrderId}`); return data; }
export async function receiveSupplierInvoice(id: number): Promise<SupplierInvoiceResponse> { const { data } = await api.post<SupplierInvoiceResponse>(`/invoices/suppliers/${id}/receive`); return data; }
export async function cancelSupplierInvoice(id: number): Promise<SupplierInvoiceResponse> { const { data } = await api.post<SupplierInvoiceResponse>(`/invoices/suppliers/${id}/cancel`); return data; }
