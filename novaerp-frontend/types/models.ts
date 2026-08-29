// types/models.ts
// Contract for the frontend. Align field names with Salma's Spring Boot DTOs.

// ---------- Base ----------
export interface BaseEntity {
  id: number;
  createdAt: string; // ISO date
  updatedAt: string;
}

// ---------- Auth / Users ----------
export type Role = 'ADMIN' | 'USER';

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// ---------- Client ----------
export interface Client extends BaseEntity {
  nom: string;
  email: string | null;
  telephone: string | null;
  adresse: string | null;
}

// ---------- Product ----------
export interface Product extends BaseEntity {
  nom: string;
  reference: string; // SKU
  prixAchat: number;
  prixVente: number;
  quantiteStock: number;
  seuilMinimum: number;
  categorie: string | null;
}

// ---------- Invoice ----------
export type InvoiceStatus = 'BROUILLON' | 'VALIDEE' | 'ANNULEE';

export interface InvoiceLine {
  id: number;
  productId: number;
  productNom: string;
  quantite: number;
  prixUnitaire: number;
  total: number;
}

export interface Invoice extends BaseEntity {
  numero: string; // FAC-2026-0001
  clientId: number;
  clientNom: string;
  lignes: InvoiceLine[];
  totalHT: number;
  tva: number;
  totalTTC: number;
  statut: InvoiceStatus;
}

// ---------- Stock ----------
export type MovementType = 'ENTREE' | 'SORTIE';
export type MovementReason = 'ACHAT' | 'VENTE' | 'CORRECTION' | 'RETOUR';

export interface StockMovement extends BaseEntity {
  productId: number;
  productNom: string;
  type: MovementType;
  motif: MovementReason;
  quantite: number;
  stockApres: number;
  userId: number;
  username: string;
}

// ---------- Alerts ----------
export interface StockAlert extends BaseEntity {
  productId: number;
  productNom: string;
  quantiteActuelle: number;
  seuilMinimum: number;
  lue: boolean;
}

// ---------- Audit ----------
export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE';
export type EntityType = 'CLIENT' | 'PRODUCT' | 'INVOICE' | 'USER' | 'STOCK';

export interface AuditLog extends BaseEntity {
  userId: number;
  username: string;
  action: AuditAction;
  entityType: EntityType;
  entityId: number;
  ancienneValeur: string | null; // JSON string
  nouvelleValeur: string | null;
}

// ---------- Pagination (matches Spring Boot Page<T>) ----------
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-based in Spring)
  size: number;
}

// ---------- Stock module (real backend, /api/stock/**) ----------
// List endpoints are paginated — see Page<T> above.

// ---- Categories ----
export interface CategoryRequest {
  name: string;
  description: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
}

// ---- Units ----
export interface UnitRequest {
  name: string;
  symbol: string;
}

export interface UnitResponse {
  id: number;
  name: string;
  symbol: string;
}

// ---- Suppliers ----
export interface SupplierRequest {
  name: string;
  email: string;
  phone: string;
  address: string;
}

export interface SupplierResponse {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  createdAt: string;
}

// ---- Articles ----
export interface ArticleRequest {
  reference: string;
  designation: string;
  brand: string;
  barcode: string;
  categoryId: number | null;
  unitId: number | null;
  purchasePriceHt: number;
  unitCostTtc: number;
  salePriceHt: number;
  minStockQuantity: number;
  serialTracked: boolean;
  description: string;
  notes: string;
}

export interface ArticleResponse {
  id: number;
  reference: string;
  designation: string;
  brand: string | null;
  barcode: string | null;
  categoryId: number | null;
  categoryName: string | null;
  unitId: number | null;
  unitName: string | null;
  purchasePriceHt: number;
  unitCostTtc: number;
  salePriceHt: number;
  stockQuantity: number;
  minStockQuantity: number;
  serialTracked: boolean;
  description: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

// ---- Article supplier prices ----
export interface ArticleSupplierPriceRequest {
  supplierId: number;
  primary?: boolean;
  currency?: string;
  priceHt: number;
  taxRate?: number;
  priceTtc: number;
  leadTimeDays: number | null;
  quoteDate: string | null; // LocalDate ISO (YYYY-MM-DD)
}

export interface ArticleSupplierPriceResponse {
  id: number;
  articleId: number;
  supplierId: number;
  supplierName: string;
  primary: boolean;
  currency: string;
  priceHt: number;
  taxRate: number;
  priceTtc: number;
  leadTimeDays: number | null;
  quoteDate: string | null;
  createdAt: string;
}

// ---- CSV import / export ----
export interface ImportRowIssue {
  row: number;
  identifier: string;
  message: string;
}

export interface ImportResultResponse {
  created: number;
  skipped: number;
  failed: number;
  errors: ImportRowIssue[];
  warnings: ImportRowIssue[];
}

// ---- Stock movements ----
export type StockMovementType = "IN" | "OUT" | "ADJUSTMENT";

export interface StockMovementRequest {
  articleId: number;
  type: StockMovementType;
  quantity: number;
  reference: string;
  note: string;
}

export interface StockMovementResponse {
  id: number;
  articleId: number;
  articleReference: string;
  type: StockMovementType;
  quantity: number;
  reference: string | null;
  note: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
}
