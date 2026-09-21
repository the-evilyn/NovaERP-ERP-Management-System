# NovaERP — Plateforme Intelligente de Gestion Commerciale, de Stock et d’Aide à la Décision

> **Projet de Fin d'Études (PFE / PFA)**  
> *Sujet :* Conception et Développement d’une Plateforme Intelligente de Gestion Commerciale, de Stock et d’Aide à la Décision pour les PME Industrielles.

---

## 1. Objectifs & Présentation du Projet

**NovaERP** est un système ERP modulaire, haute performance et ergonomique, spécialement dimensionné pour les PME industrielles marocaines et internationales. Il répond aux problématiques critiques de :
- **Traçabilité totale des stocks** et mouvements multi-types (Entrées, Sorties, Ajustements d'inventaire signés).
- **Gestion des relations partenaires** (Fournisseurs industriels & Clients grands comptes/PME).
- **Aide à la décision intelligente** et calcul déterministe et explicable du risque de rupture de stock.
- **Pilotage opérationnel en temps réel** par des indicateurs agrégés au niveau base de données (sur un catalogue de plus de 5 600 articles réels).

---

## 2. Architecture & Technologies

```
┌────────────────────────────────────────────────────────┐
│               Frontend (Next.js 16 + React 19)         │
│  Tailwind CSS • Hugeicons • TanStack Query • Axios     │
│                     (Port 3000)                        │
└───────────────────────────┬────────────────────────────┘
                            │ REST / JSON (JWT Bearer)
┌───────────────────────────▼────────────────────────────┐
│          Backend (Spring Boot 4 + Java 21)             │
│   Spring Security 6 (RBAC) • Spring Data JPA • Lombok  │
│                     (Port 8081)                        │
└─────────────┬───────────────────────────┬──────────────┘
              │                           │
┌─────────────▼─────────────┐ ┌───────────▼──────────────┐
│   PostgreSQL 16 (Alpine)  │ │   MailHog (SMTP Testing) │
│ 5 604 articles • 3 939 mvt│ │    SMTP : 1025           │
│        (Port 5433)        │ │    Web UI : 8025         │
└───────────────────────────┘ └──────────────────────────┘
```

- **Frontend :** Next.js 16.3 (Turbopack, App Router), React 19, TypeScript, Tailwind CSS, Base UI, TanStack Query v5.
- **Backend :** Spring Boot 4.1.0, Java 21 LTS, Maven, Spring Security (JWT sans état), Spring Data JPA / Hibernate ORM, springdoc-openapi (Swagger).
- **Base de données :** PostgreSQL 16 Alpine, indexé et optimisé pour l'agrégation SQL en temps réel.
- **Infrastructure :** Docker & Docker Compose.

---

## 3. Matrice de Sécurité & RBAC

L'authentification est basée sur des jetons JWT HMAC-SHA256 avec politique RBAC stricte :

| Rôle | Périmètre d'accès |
|---|---|
| **ROLE_ADMIN** | Accès complet : création/modification/suppression articles, catégories, unités, fournisseurs, clients, import/export CSV, mouvements de stock et validation des réapprovisionnements. |
| **ROLE_USER** (Opérateur) | Consultation du catalogue, visualisation des KPI, enregistrement des mouvements de stock (entrées, sorties de chaîne, inventaires), accès aux alertes d'aide à la décision. |

### Comptes de Démonstration (inclus dans la base) :
- **Administrateur :** `admin@novaerp.local` / `Admin123!` (Rôle: `ROLE_ADMIN`)
- **Opérateur de stock :** `ali.ibrahim@novaerp.local` / `User1234!` (Rôle: `ROLE_USER`)

---

## 4. Modules Fonctionnels

1. **Dashboard & KPIs Globaux (`/dashboard`) :**
   - Calcul agrégé natif PostgreSQL sur l'ensemble de la base (5 604 articles) en temps réel.
   - Valeur totale du stock valorisée au prix d'achat HT (plus de 8,65 Millions MAD).
   - Décompte instantané des ruptures, stocks critiques et stocks faibles.
   - Graphique de répartition de la valeur par catégorie industrielle.
   - Suivi des 8 articles les plus valorisés et flux des mouvements récents.
   - Carte d'alerte prioritaire d'aide à la décision avec réapprovisionnement direct.

2. **Moteur Intelligent d'Aide à la Décision (`/decisions`) :**
   - **Score de vulnérabilité (0 à 100%) :**
     - $Stock \le 0$ : Rupture totale (Score: 100%, niveau `OUT_OF_STOCK`).
     - $\frac{Stock}{Seuil} \le 0.33$ : Niveau `CRITICAL` (Score: 70–99%).
     - $\frac{Stock}{Seuil} \le 1.0$ : Niveau `WARNING` (Score: 10–69%).
   - **Quantité suggérée optimale :**
     $$Q_{\text{suggérée}} = (1.5 \times Seuil_{\text{min}}) - Stock_{\text{actuel}}$$
     Garantit un stock tampon de sécurité de +50% après livraison.
   - **Sélection optimale du fournisseur :**
     Priorité au fournisseur principal assigné (`isPrimary`), ou sélection du meilleur devis actif (compromis prix HT et délai de livraison en jours).
   - **Budget prévisionnel estimé :**
     $$Budget = Q_{\text{suggérée}} \times Prix_{\text{unitaire}}$$
   - **Explication en langage naturel (Français) :**
     Chaque recommandation fournit un diagnostic complet et transparent pour le responsable d'achats.
   - **Commande en 1-clic :**
     Génération automatique d'un mouvement d'entrée (`IN`) pré-rempli (`CMD-REAP-...`).

3. **Gestion des Clients Industriels (`/clients`) :**
   - Fiches complètes : Raison sociale, Email, Téléphone, Adresse, Ville, ICE / Numéro fiscal.
   - Recherche multi-champs instantanée, création, modification, suppression unitaire ou par lot.

4. **Gestion des Fournisseurs & Devis (`/suppliers`) :**
   - Suivi des fournisseurs industriels avec devis d'achat HT/TTC et délais logistiques.

5. **Gestion du Catalogue Articles (`/articles`) :**
   - Référence, Désignation, Marque, Code-barres, Catégorie, Unité, Prix d'achat, Prix de vente, Seuil de réapprovisionnement.

6. **Mouvements de Stock Traçables (`/stock-movements`) :**
   - Enregistrement des Entrées (`IN`), Sorties (`OUT`), et Ajustements d'inventaire signés (`ADJUSTMENT`).
   - Protection stricte contre les stocks négatifs en sortie.
   - Audit automatique : horodatage et nom de l'opérateur ayant saisi l'opération.

7. **Import / Export CSV (`/import-export`) :**
   - Importation et exportation en masse pour articles, catégories et fournisseurs avec gestion de tolérance aux doublons.

---

## 5. Démarrage Rapide

### Prérequis
- Docker et Docker Compose
- Node.js 20+ et npm
- Java 21 & Maven (optionnel si exécution hors Docker)

### 1. Démarrer l'infrastructure (Base PostgreSQL + MailHog)
```bash
cd novaerp-backend
docker-compose up -d postgres mailhog
```
*La base PostgreSQL est accessible sur l'hôte au port `5433` (utilisateur: `novaerp`, mot de passe: `novaerp`, base: `novaerp`).*

### 2. Démarrer le Backend Spring Boot
```bash
cd novaerp-backend
DB_PORT=5433 ./mvnw spring-boot:run
```
*Le backend démarre sur `http://localhost:8081`.*
- Swagger API Docs : [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

### 3. Démarrer le Frontend Next.js
```bash
cd novaerp-frontend
npm install
npm run dev
```
*L'application est accessible sur [http://localhost:3000](http://localhost:3000).*

---

## 6. Commandes de Validation & Tests

### Backend (45 tests unitaires et d'intégration) :
```bash
cd novaerp-backend
./mvnw clean test
```

### Frontend (Linting et compilation de production) :
```bash
cd novaerp-frontend
npm run lint    # Doit afficher : 0 errors, 0 warnings
npm run build   # Génère les 17 routes statiques avec succès
```

---

## 7. Scénario Recommandé de Soutenance PFE (5 à 10 minutes)

1. **Introduction & Contexte (1 min) :**
   - Présentation de la problématique des PME industrielles : ruptures imprévues de stocks de pièces critiques, surstocks coûteux, manque de traçabilité.
   - Présentation de l'architecture moderne découplée (Next.js 16 + Spring Boot 4 + PostgreSQL).

2. **Authentification & Contrôle d'Accès RBAC (1 min) :**
   - Connexion en tant qu'Administrateur (`admin@novaerp.local`).
   - Présentation des rôles et de la sécurisation JWT sans état.

3. **Dashboard Analytique Haute Performance (2 min) :**
   - Démonstration de l'agrégation SQL en temps réel sur les 5 604 articles (plus de 8,65M MAD de stock total).
   - Répartition dynamique de la valeur par catégorie industrielle (Électrique, Pneumatique, Automatisme...).
   - Présentation des indicateurs de santé du stock (ruptures, critique, faible).

4. **Module d'Aide à la Décision & Algorithme Intelligent (3 min) :**
   - Navigation vers la section *Aide à la décision* (`/decisions`).
   - Explication de l'algorithme déterministe :
     - Score de risque (0 à 100%) basé sur le ratio stock actuel / seuil minimum.
     - Dimensionnement du réapprovisionnement avec stock tampon de sécurité (+50%).
     - Sélection automatisée du meilleur fournisseur d'après les devis et délais.
     - Estimation prévisionnelle du budget total nécessaire.
     - Diagnostic explicatif transparent en français.
   - Action concrète : Clic sur *"Commander"* sur un article en rupture -> validation immédiate du bon d'entrée en stock -> mise à jour instantanée du niveau de stock et des KPIs.

5. **Gestion Opérationnelle & Traçabilité (2 min) :**
   - Présentation du module *Clients* (`/clients`) avec recherche instantanée.
   - Présentation de l'enregistrement d'un mouvement de stock (protection contre le stock négatif en sortie).
   - Connexion rapide avec le compte opérateur (`ali.ibrahim@novaerp.local`) pour démontrer la restriction des droits d'administration (lecture + mouvements autorisés, administration bloquée).

6. **Conclusion & Perspectives (1 min) :**
   - Synthèse des apports : gains de temps, zéro rupture non anticipée, visibilité financière complète.
   - Perspectives : intégration d'un modèle prédictif saisonnier (ARIMA / Prophet) pour la demande prévisionnelle.
