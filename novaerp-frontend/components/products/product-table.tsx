"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
  Alert02Icon,
  CheckmarkBadge01Icon,
  Delete02Icon,
  MoreVerticalIcon,
  PencilEdit01Icon,
  Upload01Icon,
  ViewIcon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import type React from "react";
import { useRef, useState } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Menu,
  MenuItem,
  MenuPopup,
  MenuSeparator,
  MenuTrigger,
} from "@/components/ui/menu";
import { DataTable, type TableColumn } from "@/components/shared/data-table";
import PaginationTable from "@/components/shared/pagination-table";
import { CreateProductDialog } from "@/components/products/create-product-dialog";
import { EditProductDialog } from "@/components/products/edit-product-dialog";
import { parseCsv } from "@/lib/csv";
import {
  useCreateProducts,
  useDeleteProducts,
  useProducts,
} from "@/hooks/use-products";
import type { Product } from "@/types/models";

const currency = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 0,
});

function ProductExpandedRow({
  product,
}: {
  product: Product;
}): React.ReactElement {
  return (
    <div className="p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="font-semibold text-sm">Détails du produit</h3>
        <span className="text-muted-foreground text-xs">
          Marge : {currency.format(product.prixVente - product.prixAchat)}
        </span>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
        <div className="flex flex-col gap-0.5">
          <span className="text-muted-foreground text-xs">Prix d'achat</span>
          <span className="text-sm">{currency.format(product.prixAchat)}</span>
        </div>
        <div className="flex flex-col gap-0.5">
          <span className="text-muted-foreground text-xs">Prix de vente</span>
          <span className="text-sm">{currency.format(product.prixVente)}</span>
        </div>
        <div className="flex flex-col gap-0.5">
          <span className="text-muted-foreground text-xs">
            Seuil minimum
          </span>
          <span className="text-sm">{product.seuilMinimum} unités</span>
        </div>
        <div className="flex flex-col gap-0.5">
          <span className="text-muted-foreground text-xs">Catégorie</span>
          <span className="text-sm">{product.categorie ?? "—"}</span>
        </div>
      </div>
    </div>
  );
}

export function ProductTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isPending } = useProducts(currentPage - 1, pageSize);
  const stockValue = (data?.content ?? []).reduce(
    (sum, product) => sum + product.prixVente * product.quantiteStock,
    0,
  );
  const deleteProducts = useDeleteProducts();
  const createProducts = useCreateProducts();

  const handleBulkDelete = async () => {
    await deleteProducts.mutateAsync(selectedIds.map(Number));
    setSelectedIds([]);
    setDeleteDialogOpen(false);
  };

  const handleImportCsv = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    const text = await file.text();
    const rows = parseCsv(text);

    const products = rows
      .filter((row) => row.nom && row.reference)
      .map((row) => ({
        nom: row.nom,
        reference: row.reference,
        categorie: row.categorie || null,
        prixAchat: Number(row.prixAchat) || 0,
        prixVente: Number(row.prixVente) || 0,
        quantiteStock: Number(row.quantiteStock) || 0,
        seuilMinimum: Number(row.seuilMinimum) || 0,
      }));

    if (products.length > 0) {
      await createProducts.mutateAsync(products);
    }
  };

  const columns: TableColumn<Product>[] = [
    {
      key: "product",
      label: "Produit",
      render: (product) => (
        <div className="flex max-w-64 flex-col">
          <span className="truncate font-medium">{product.nom}</span>
          <span className="truncate text-muted-foreground text-xs">
            {product.reference}
          </span>
        </div>
      ),
    },
    {
      key: "categorie",
      label: "Catégorie",
      render: (product) => (
        <span className="text-muted-foreground">
          {product.categorie ?? "—"}
        </span>
      ),
    },
    {
      key: "prixAchat",
      label: "Prix d'achat",
      render: (product) => (
        <span className="text-muted-foreground">
          {currency.format(product.prixAchat)}
        </span>
      ),
    },
    {
      key: "prixVente",
      label: "Prix de vente",
      render: (product) => (
        <span className="font-medium">{currency.format(product.prixVente)}</span>
      ),
    },
    {
      key: "stock",
      label: "Stock",
      render: (product) => (
        <span
          className={
            product.quantiteStock <= product.seuilMinimum
              ? "font-medium text-destructive"
              : ""
          }
        >
          {product.quantiteStock} u.
        </span>
      ),
    },
    {
      key: "statut",
      label: "Statut",
      render: (product) =>
        product.quantiteStock <= product.seuilMinimum ? (
          <Badge variant="warning">
            <HugeiconsIcon
              icon={Alert02Icon}
              strokeWidth={2}
              className="text-warning-foreground"
            />
            Stock faible
          </Badge>
        ) : (
          <Badge variant="secondary">
            <HugeiconsIcon
              icon={CheckmarkBadge01Icon}
              strokeWidth={2}
              className="text-success-foreground"
            />
            En stock
          </Badge>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["nom", "reference", "categorie"]}
        searchPlaceholder="Rechercher un produit par nom ou référence..."
        emptyMessage={isPending ? "Chargement..." : "Aucun produit trouvé."}
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        expandable
        renderExpandedRow={(product) => (
          <ProductExpandedRow product={product} />
        )}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Valeur du stock (page actuelle)</span>
              <span>{currency.format(stockValue)}</span>
            </div>
          ) : undefined
        }
        customHeader={
          <>
            {selectedIds.length > 0 && (
              <Button
                variant="destructive"
                onClick={() => setDeleteDialogOpen(true)}
              >
                <HugeiconsIcon icon={Delete02Icon} strokeWidth={2} />
                Supprimer ({selectedIds.length})
              </Button>
            )}
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={handleImportCsv}
            />
            <Button
              variant="outline"
              loading={createProducts.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              <HugeiconsIcon icon={Upload01Icon} strokeWidth={2} />
              Importer CSV
            </Button>
            <Button onClick={() => setCreateDialogOpen(true)}>
              <HugeiconsIcon icon={Add01Icon} strokeWidth={2} />
              Nouveau produit
            </Button>
          </>
        }
        actions={(product) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem render={<Link href={`/products/${product.id}`} />}>
                <HugeiconsIcon icon={ViewIcon} strokeWidth={2} />
                Voir la fiche
              </MenuItem>
              <MenuItem onClick={() => setEditingProduct(product)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([product.id]);
                  setDeleteDialogOpen(true);
                }}
              >
                <HugeiconsIcon icon={Delete02Icon} strokeWidth={2} />
                Supprimer
              </MenuItem>
            </MenuPopup>
          </Menu>
        )}
      />

      {data && data.totalElements > 0 && (
        <PaginationTable
          currentPage={currentPage}
          totalPages={data.totalPages}
          pageSize={pageSize}
          totalItems={data.totalElements}
          onPageChange={setCurrentPage}
          onPageSizeChange={setPageSize}
        />
      )}

      <CreateProductDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />

      <EditProductDialog
        product={editingProduct}
        open={editingProduct !== null}
        onOpenChange={(open) => {
          if (!open) setEditingProduct(null);
        }}
      />

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Supprimer {selectedIds.length} produit
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les produits sélectionnés seront
              définitivement supprimés.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleBulkDelete}
              loading={deleteProducts.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
