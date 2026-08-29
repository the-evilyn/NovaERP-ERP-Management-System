// services/services.mock.ts
// Fake API layer. Same function signatures as the real one later.
// When Salma's API is ready: replace the bodies with axios calls — hooks & pages don't change.

import type {
  Client,
  Product,
  Invoice,
  StockMovement,
  StockAlert,
  AuditLog,
  Page,
} from '@/types/models';
import {
  mockClients,
  mockProducts,
  mockInvoices,
  mockStockMovements,
  mockAlerts,
  mockAuditLogs,
  toPage,
  delay,
} from '@/mocks/mock-data';

// ---------- Clients ----------
export async function getClients(page = 0, size = 10): Promise<Page<Client>> {
  await delay();
  return toPage(mockClients, page, size);
}

export async function getClient(id: number): Promise<Client> {
  await delay();
  const client = mockClients.find((c) => c.id === id);
  if (!client) throw new Error('Client introuvable');
  return client;
}

export async function deleteClients(ids: number[]): Promise<void> {
  await delay();
  const idSet = new Set(ids);
  const remaining = mockClients.filter((c) => !idSet.has(c.id));
  mockClients.splice(0, mockClients.length, ...remaining);
}

export async function createClients(
  clients: Omit<Client, 'id' | 'createdAt' | 'updatedAt'>[],
): Promise<Client[]> {
  await delay();
  const now = new Date().toISOString();
  let nextId = mockClients.reduce((max, c) => Math.max(max, c.id), 0);
  const created = clients.map((c) => ({
    ...c,
    id: ++nextId,
    createdAt: now,
    updatedAt: now,
  }));
  mockClients.push(...created);
  return created;
}

export async function updateClient(
  id: number,
  data: Omit<Client, 'id' | 'createdAt' | 'updatedAt'>,
): Promise<Client> {
  await delay();
  const client = mockClients.find((c) => c.id === id);
  if (!client) throw new Error('Client introuvable');
  Object.assign(client, data, { updatedAt: new Date().toISOString() });
  return client;
}

// ---------- Products ----------
export async function getProducts(page = 0, size = 10): Promise<Page<Product>> {
  await delay();
  return toPage(mockProducts, page, size);
}

export async function getProduct(id: number): Promise<Product> {
  await delay();
  const product = mockProducts.find((p) => p.id === id);
  if (!product) throw new Error('Produit introuvable');
  return product;
}

export async function deleteProducts(ids: number[]): Promise<void> {
  await delay();
  const idSet = new Set(ids);
  const remaining = mockProducts.filter((p) => !idSet.has(p.id));
  mockProducts.splice(0, mockProducts.length, ...remaining);
}

export async function createProducts(
  products: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>[],
): Promise<Product[]> {
  await delay();
  const now = new Date().toISOString();
  let nextId = mockProducts.reduce((max, p) => Math.max(max, p.id), 0);
  const created = products.map((p) => ({
    ...p,
    id: ++nextId,
    createdAt: now,
    updatedAt: now,
  }));
  mockProducts.push(...created);
  return created;
}

export async function updateProduct(
  id: number,
  data: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>,
): Promise<Product> {
  await delay();
  const product = mockProducts.find((p) => p.id === id);
  if (!product) throw new Error('Produit introuvable');
  Object.assign(product, data, { updatedAt: new Date().toISOString() });
  return product;
}

// ---------- Invoices ----------
export async function getInvoices(page = 0, size = 10): Promise<Page<Invoice>> {
  await delay();
  return toPage(mockInvoices, page, size);
}

export async function getInvoice(id: number): Promise<Invoice> {
  await delay();
  const invoice = mockInvoices.find((i) => i.id === id);
  if (!invoice) throw new Error('Facture introuvable');
  return invoice;
}

// ---------- Stock ----------
export async function getStockMovements(
  page = 0,
  size = 10,
): Promise<Page<StockMovement>> {
  await delay();
  return toPage(mockStockMovements, page, size);
}

// ---------- Alerts ----------
export async function getAlerts(): Promise<StockAlert[]> {
  await delay();
  return mockAlerts;
}

// ---------- Audit ----------
export async function getAuditLogs(
  page = 0,
  size = 10,
): Promise<Page<AuditLog>> {
  await delay();
  return toPage(mockAuditLogs, page, size);
}
