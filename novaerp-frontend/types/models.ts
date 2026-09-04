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
export interface ClientResponse extends BaseEntity {
  name?: string;
  nom: string;
  email: string | null;
  phone?: string | null;
  telephone?: string | null;
  address?: string | null;
  adresse?: string | null;
  city?: string | null;
  taxNumber?: string | null;
  notes?: string | null;
}

export type Client = ClientResponse;

export interface ClientRequest {
  name?: string;
  nom?: string;
  email?: string | null;
  phone?: string | null;
  telephone?: string | null;
  address?: string | null;
  adresse?: string | null;
  city?: string | null;
  taxNumber?: string | null;
  notes?: string | null;
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

// ---- Dashboard Stats ----
export interface CategoryStockValue {
  categoryName: string;
  value: number;
}

export interface TopArticleStockValue {
  id: number;
  reference: string;
  designation: string;
  stockQuantity: number;
  purchasePriceHt: number;
  stockValue: number;
  unitName: string | null;
}

export interface DashboardStatsResponse {
  totalArticles: number;
  totalCategories: number;
  totalSuppliers: number;
  totalClients: number;
  totalQuantity: number;
  totalValue: number;
  criticalStock: number;
  lowStock: number;
  outOfStock: number;
  categoryValues: CategoryStockValue[];
  topArticles: TopArticleStockValue[];
}

// ---- Intelligent Decision Support ----
export type RiskLevel = "OUT_OF_STOCK" | "CRITICAL" | "WARNING" | "NORMAL";

export interface ReorderRecommendationResponse {
  articleId: number;
  articleReference: string;
  designation: string;
  categoryName: string | null;
  unitName: string | null;
  currentStock: number;
  minStockQuantity: number;
  riskScore: number;
  riskLevel: RiskLevel;
  suggestedQuantity: number;
  recommendedSupplierId: number | null;
  recommendedSupplierName: string;
  unitPrice: number;
  leadTimeDays: number | null;
  estimatedBudget: number;
  explanation: string;
}

export interface StockRiskSummaryResponse {
  totalArticlesAtRisk: number;
  outOfStockCount: number;
  criticalCount: number;
  warningCount: number;
  totalEstimatedReorderBudget: number;
  averageRiskScore: number;
}

// ---- Sales Orders ----
export type SaleOrderStatus = "DRAFT" | "CONFIRMED" | "DELIVERED" | "CANCELLED";

export interface SaleOrderItemRequest {
  articleId: number;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
}

export interface SaleOrderRequest {
  clientId: number;
  items: SaleOrderItemRequest[];
  taxRate?: number;
  notes?: string;
}

export interface SaleOrderItemResponse {
  id: number;
  articleId: number;
  articleReference: string;
  articleDesignation: string;
  unitSymbol: string | null;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  totalHt: number;
  totalTtc: number;
}

export interface SaleOrderResponse {
  id: number;
  orderNumber: string;
  clientId: number;
  clientName: string;
  clientCity: string | null;
  status: SaleOrderStatus;
  subtotalHt: number;
  taxRate: number;
  taxAmount: number;
  totalTtc: number;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  confirmedAt: string | null;
  deliveredAt: string | null;
  items: SaleOrderItemResponse[];
}

// ---- Purchase Orders ----
export type PurchaseOrderStatus = "DRAFT" | "CONFIRMED" | "RECEIVED" | "CANCELLED";

export interface PurchaseOrderItemRequest {
  articleId: number;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
}

export interface PurchaseOrderRequest {
  supplierId: number;
  items: PurchaseOrderItemRequest[];
  taxRate?: number;
  notes?: string;
}

export interface PurchaseOrderItemResponse {
  id: number;
  articleId: number;
  articleReference: string;
  articleDesignation: string;
  unitSymbol: string | null;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  totalHt: number;
  totalTtc: number;
}

export interface PurchaseOrderResponse {
  id: number;
  orderNumber: string;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  subtotalHt: number;
  taxRate: number;
  taxAmount: number;
  totalTtc: number;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  confirmedAt: string | null;
  receivedAt: string | null;
  items: PurchaseOrderItemResponse[];
}

// ---- Invoices ----
export type CustomerInvoiceStatus = "DRAFT" | "ISSUED" | "PAID" | "CANCELLED";
export type SupplierInvoiceStatus = "DRAFT" | "RECEIVED" | "PAID" | "CANCELLED";

export interface CustomerInvoiceItemRequest {
  articleId: number;
  quantity: number;
  unitPrice: number;
  taxRate?: number;
}

export type SupplierInvoiceItemRequest = CustomerInvoiceItemRequest;

export interface CustomerInvoiceRequest {
  clientId: number;
  saleOrderId?: number;
  items: CustomerInvoiceItemRequest[];
  taxRate?: number;
  notes?: string;
}

export interface SupplierInvoiceRequest {
  supplierId: number;
  purchaseOrderId?: number;
  items: SupplierInvoiceItemRequest[];
  taxRate?: number;
  notes?: string;
}

export interface InvoiceItemResponse {
  id: number;
  articleId: number;
  articleReference: string;
  articleDesignation: string;
  unitSymbol: string | null;
  quantity: number;
  unitPrice: number;
  taxRate: number | null;
  totalHt: number;
  totalTtc: number;
}

export interface CustomerInvoiceResponse {
  id: number;
  invoiceNumber: string;
  clientId: number;
  clientName: string;
  clientCity: string | null;
  saleOrderId: number | null;
  saleOrderNumber: string | null;
  status: CustomerInvoiceStatus;
  subtotalHt: number;
  taxRate: number | null;
  taxAmount: number;
  totalTtc: number;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  issuedAt: string | null;
  paidAt: string | null;
  cancelledAt: string | null;
  items: InvoiceItemResponse[];
}

export interface SupplierInvoiceResponse {
  id: number;
  invoiceNumber: string;
  supplierId: number;
  supplierName: string;
  purchaseOrderId: number | null;
  purchaseOrderNumber: string | null;
  status: SupplierInvoiceStatus;
  subtotalHt: number;
  taxRate: number | null;
  taxAmount: number;
  totalTtc: number;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  receivedAt: string | null;
  paidAt: string | null;
  cancelledAt: string | null;
  items: InvoiceItemResponse[];
}

// ---- Payments ----
export type PaymentMethod = "CASH" | "BANK_TRANSFER" | "CHECK" | "CREDIT_CARD" | "OTHER";
export type PaymentType = "CUSTOMER_PAYMENT" | "SUPPLIER_PAYMENT";

export interface CustomerPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  paymentDate?: string;
  referenceNumber?: string;
  notes?: string;
}

export type SupplierPaymentRequest = CustomerPaymentRequest;

export interface PaymentResponse {
  id: number;
  paymentNumber: string;
  paymentType: PaymentType;
  customerInvoiceId: number | null;
  customerInvoiceNumber: string | null;
  supplierInvoiceId: number | null;
  supplierInvoiceNumber: string | null;
  paymentMethod: PaymentMethod;
  amount: number;
  paymentDate: string;
  referenceNumber: string | null;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
}

export interface InvoicePaymentSummaryResponse {
  invoiceId: number;
  invoiceNumber: string;
  invoiceType: string;
  invoiceStatus: string;
  totalTtc: number;
  totalPaid: number;
  remainingAmount: number;
  isFullyPaid: boolean;
  payments: PaymentResponse[];
}
