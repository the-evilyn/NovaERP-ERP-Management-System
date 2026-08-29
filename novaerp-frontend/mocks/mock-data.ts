// mocks/mock-data.ts
// Fake data matching the models. Delete this folder when Salma's API is ready.

import type {
  User,
  Client,
  Product,
  Invoice,
  StockMovement,
  StockAlert,
  AuditLog,
  Page,
} from '@/types/models';

const now = new Date().toISOString();
const daysAgo = (n: number) =>
  new Date(Date.now() - n * 24 * 60 * 60 * 1000).toISOString();

// ---------- Users ----------
export const mockUsers: User[] = [
  {
    id: 1,
    fullName: 'Salma',
    email: 'salma@novaerp.ma',
    role: 'ADMIN',
  },
  {
    id: 2,
    fullName: 'Demo',
    email: 'demo@novaerp.ma',
    role: 'USER',
  },
];

// ---------- Clients ----------
export const mockClients: Client[] = [
  {
    id: 1,
    nom: 'Société Atlas Distribution',
    email: 'contact@atlas-dist.ma',
    telephone: '0522334455',
    adresse: 'Zone industrielle, Casablanca',
    createdAt: daysAgo(45),
    updatedAt: daysAgo(5),
  },
  {
    id: 2,
    nom: 'Marjane Market Kenitra',
    email: 'achats@marjane.ma',
    telephone: '0537778899',
    adresse: 'Av. Mohammed V, Kénitra',
    createdAt: daysAgo(40),
    updatedAt: daysAgo(2),
  },
  {
    id: 3,
    nom: 'Épicerie Al Baraka',
    email: null,
    telephone: '0661234567',
    adresse: 'Quartier Saknia, Kénitra',
    createdAt: daysAgo(20),
    updatedAt: daysAgo(20),
  },
  {
    id: 4,
    nom: 'Café Restaurant Océan',
    email: 'ocean.resto@gmail.com',
    telephone: '0668889900',
    adresse: 'Corniche, Mehdia',
    createdAt: daysAgo(10),
    updatedAt: daysAgo(1),
  },
];

// ---------- Products ----------
export const mockProducts: Product[] = [
  {
    id: 1,
    nom: 'Huile de table 5L',
    reference: 'HUI-005',
    prixAchat: 85,
    prixVente: 105,
    quantiteStock: 120,
    seuilMinimum: 30,
    categorie: 'Alimentation',
    createdAt: daysAgo(50),
    updatedAt: daysAgo(1),
  },
  {
    id: 2,
    nom: 'Sucre 2kg',
    reference: 'SUC-002',
    prixAchat: 18,
    prixVente: 24,
    quantiteStock: 8, // below threshold -> alert
    seuilMinimum: 25,
    categorie: 'Alimentation',
    createdAt: daysAgo(50),
    updatedAt: now,
  },
  {
    id: 3,
    nom: 'Farine 10kg',
    reference: 'FAR-010',
    prixAchat: 55,
    prixVente: 72,
    quantiteStock: 60,
    seuilMinimum: 15,
    categorie: 'Alimentation',
    createdAt: daysAgo(48),
    updatedAt: daysAgo(3),
  },
  {
    id: 4,
    nom: 'Eau minérale pack 6x1.5L',
    reference: 'EAU-006',
    prixAchat: 22,
    prixVente: 30,
    quantiteStock: 3, // below threshold -> alert
    seuilMinimum: 20,
    categorie: 'Boissons',
    createdAt: daysAgo(35),
    updatedAt: now,
  },
  {
    id: 5,
    nom: 'Détergent 3L',
    reference: 'DET-003',
    prixAchat: 28,
    prixVente: 39,
    quantiteStock: 45,
    seuilMinimum: 10,
    categorie: 'Entretien',
    createdAt: daysAgo(30),
    updatedAt: daysAgo(15),
  },
  {
    id: 6,
    nom: 'Thé vert 500g',
    reference: 'THE-500',
    prixAchat: 40,
    prixVente: 58,
    quantiteStock: 90,
    seuilMinimum: 20,
    categorie: 'Boissons',
    createdAt: daysAgo(60),
    updatedAt: daysAgo(55), // dormant: no movement for a long time
  },
];

// ---------- Invoices ----------
export const mockInvoices: Invoice[] = [
  {
    id: 1,
    numero: 'FAC-2026-0001',
    clientId: 1,
    clientNom: 'Société Atlas Distribution',
    lignes: [
      {
        id: 1,
        productId: 1,
        productNom: 'Huile de table 5L',
        quantite: 20,
        prixUnitaire: 105,
        total: 2100,
      },
      {
        id: 2,
        productId: 3,
        productNom: 'Farine 10kg',
        quantite: 10,
        prixUnitaire: 72,
        total: 720,
      },
    ],
    totalHT: 2820,
    tva: 564,
    totalTTC: 3384,
    statut: 'VALIDEE',
    createdAt: daysAgo(7),
    updatedAt: daysAgo(7),
  },
  {
    id: 2,
    numero: 'FAC-2026-0002',
    clientId: 2,
    clientNom: 'Marjane Market Kenitra',
    lignes: [
      {
        id: 3,
        productId: 2,
        productNom: 'Sucre 2kg',
        quantite: 50,
        prixUnitaire: 24,
        total: 1200,
      },
    ],
    totalHT: 1200,
    tva: 240,
    totalTTC: 1440,
    statut: 'VALIDEE',
    createdAt: daysAgo(3),
    updatedAt: daysAgo(3),
  },
  {
    id: 3,
    numero: 'FAC-2026-0003',
    clientId: 4,
    clientNom: 'Café Restaurant Océan',
    lignes: [
      {
        id: 4,
        productId: 4,
        productNom: 'Eau minérale pack 6x1.5L',
        quantite: 15,
        prixUnitaire: 30,
        total: 450,
      },
    ],
    totalHT: 450,
    tva: 90,
    totalTTC: 540,
    statut: 'BROUILLON',
    createdAt: daysAgo(1),
    updatedAt: now,
  },
];

// ---------- Stock movements ----------
export const mockStockMovements: StockMovement[] = [
  {
    id: 1,
    productId: 1,
    productNom: 'Huile de table 5L',
    type: 'ENTREE',
    motif: 'ACHAT',
    quantite: 100,
    stockApres: 140,
    userId: 1,
    username: 'salma',
    createdAt: daysAgo(10),
    updatedAt: daysAgo(10),
  },
  {
    id: 2,
    productId: 1,
    productNom: 'Huile de table 5L',
    type: 'SORTIE',
    motif: 'VENTE',
    quantite: 20,
    stockApres: 120,
    userId: 2,
    username: 'demo',
    createdAt: daysAgo(7),
    updatedAt: daysAgo(7),
  },
  {
    id: 3,
    productId: 2,
    productNom: 'Sucre 2kg',
    type: 'SORTIE',
    motif: 'VENTE',
    quantite: 50,
    stockApres: 8,
    userId: 2,
    username: 'demo',
    createdAt: daysAgo(3),
    updatedAt: daysAgo(3),
  },
  {
    id: 4,
    productId: 4,
    productNom: 'Eau minérale pack 6x1.5L',
    type: 'SORTIE',
    motif: 'CORRECTION',
    quantite: 2,
    stockApres: 3,
    userId: 1,
    username: 'salma',
    createdAt: daysAgo(1),
    updatedAt: daysAgo(1),
  },
];

// ---------- Alerts ----------
export const mockAlerts: StockAlert[] = [
  {
    id: 1,
    productId: 2,
    productNom: 'Sucre 2kg',
    quantiteActuelle: 8,
    seuilMinimum: 25,
    lue: false,
    createdAt: daysAgo(3),
    updatedAt: daysAgo(3),
  },
  {
    id: 2,
    productId: 4,
    productNom: 'Eau minérale pack 6x1.5L',
    quantiteActuelle: 3,
    seuilMinimum: 20,
    lue: false,
    createdAt: daysAgo(1),
    updatedAt: daysAgo(1),
  },
];

// ---------- Audit ----------
export const mockAuditLogs: AuditLog[] = [
  {
    id: 1,
    userId: 1,
    username: 'salma',
    action: 'CREATE',
    entityType: 'PRODUCT',
    entityId: 6,
    ancienneValeur: null,
    nouvelleValeur: JSON.stringify({ nom: 'Thé vert 500g', prixVente: 58 }),
    createdAt: daysAgo(60),
    updatedAt: daysAgo(60),
  },
  {
    id: 2,
    userId: 2,
    username: 'demo',
    action: 'UPDATE',
    entityType: 'CLIENT',
    entityId: 2,
    ancienneValeur: JSON.stringify({ telephone: '0537000000' }),
    nouvelleValeur: JSON.stringify({ telephone: '0537778899' }),
    createdAt: daysAgo(2),
    updatedAt: daysAgo(2),
  },
  {
    id: 3,
    userId: 2,
    username: 'demo',
    action: 'CREATE',
    entityType: 'INVOICE',
    entityId: 3,
    ancienneValeur: null,
    nouvelleValeur: JSON.stringify({ numero: 'FAC-2026-0003', totalTTC: 540 }),
    createdAt: daysAgo(1),
    updatedAt: daysAgo(1),
  },
];

// ---------- Helper: wrap any array in Spring-style Page<T> ----------
export function toPage<T>(items: T[], page = 0, size = 10): Page<T> {
  const start = page * size;
  return {
    content: items.slice(start, start + size),
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
    number: page,
    size,
  };
}

// ---------- Helper: fake network delay ----------
export const delay = (ms = 400) =>
  new Promise((resolve) => setTimeout(resolve, ms));
