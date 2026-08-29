"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
  Alert02Icon,
  CheckmarkBadge01Icon,
  AlertCircleIcon,
  Delete02Icon,
  MoreVerticalIcon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
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
import { Alert, AlertDescription } from "@/components/ui/alert";
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
import { ArticleSupplierPrices } from "@/components/articles/article-supplier-prices";
import { CreateArticleDialog } from "@/components/articles/create-article-dialog";
import { EditArticleDialog } from "@/components/articles/edit-article-dialog";
import { useArticles, useDeleteArticle } from "@/hooks/use-articles";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ArticleResponse } from "@/types/models";

const currency = new Intl.NumberFormat("fr-MA", {
  style: "currency",
  currency: "MAD",
  maximumFractionDigits: 2,
});

export function ArticleTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingArticle, setEditingArticle] = useState<ArticleResponse | null>(
    null,
  );

  const { data, isPending } = useArticles(currentPage - 1, pageSize);
  const deleteArticle = useDeleteArticle();

  const handleBulkDelete = async () => {
    setDeleteError(null);
    try {
      for (const id of selectedIds) {
        await deleteArticle.mutateAsync(Number(id));
      }
      setSelectedIds([]);
      setDeleteDialogOpen(false);
    } catch (err) {
      setDeleteError(getApiErrorMessage(err, "Impossible de supprimer l'article."));
    }
  };

  const columns: TableColumn<ArticleResponse>[] = [
    {
      key: "article",
      label: "Article",
      render: (article) => (
        <div className="flex max-w-64 flex-col">
          <span className="truncate font-medium">{article.designation}</span>
          <span className="truncate text-muted-foreground text-xs">
            {article.reference}
          </span>
        </div>
      ),
    },
    {
      key: "categoryName",
      label: "Catégorie",
      render: (article) => (
        <span className="text-muted-foreground">
          {article.categoryName ?? "—"}
        </span>
      ),
    },
    {
      key: "unitName",
      label: "Unité",
      render: (article) => (
        <span className="text-muted-foreground">
          {article.unitName ?? "—"}
        </span>
      ),
    },
    {
      key: "purchasePriceHt",
      label: "Prix d'achat HT",
      render: (article) => (
        <span className="text-muted-foreground">
          {currency.format(article.purchasePriceHt)}
        </span>
      ),
    },
    {
      key: "salePriceHt",
      label: "Prix de vente HT",
      render: (article) => (
        <span className="font-medium">
          {currency.format(article.salePriceHt)}
        </span>
      ),
    },
    {
      key: "stockQuantity",
      label: "Stock",
      render: (article) => (
        <span
          className={
            article.stockQuantity <= article.minStockQuantity
              ? "font-medium text-destructive"
              : ""
          }
        >
          {article.stockQuantity} {article.unitName ?? "u."}
        </span>
      ),
    },
    {
      key: "statut",
      label: "Statut",
      render: (article) =>
        article.stockQuantity <= article.minStockQuantity ? (
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
        searchKeys={["designation", "reference", "categoryName", "brand"]}
        searchPlaceholder="Rechercher un article par désignation ou référence..."
        emptyMessage={isPending ? "Chargement..." : "Aucun article trouvé."}
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        expandable
        renderExpandedRow={(article) => (
          <ArticleSupplierPrices article={article} />
        )}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total articles</span>
              <span>{data.totalElements}</span>
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
            <Button onClick={() => setCreateDialogOpen(true)}>
              <HugeiconsIcon icon={Add01Icon} strokeWidth={2} />
              Nouvel article
            </Button>
          </>
        }
        actions={(article) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem onClick={() => setEditingArticle(article)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([article.id]);
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

      <CreateArticleDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />

      <EditArticleDialog
        article={editingArticle}
        open={editingArticle !== null}
        onOpenChange={(open) => {
          if (!open) setEditingArticle(null);
        }}
      />

      <AlertDialog
        open={deleteDialogOpen}
        onOpenChange={(open) => {
          setDeleteDialogOpen(open);
          if (!open) setDeleteError(null);
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Supprimer {selectedIds.length} article
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les articles sélectionnés seront
              définitivement supprimés.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteError && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} />
              <AlertDescription>{deleteError}</AlertDescription>
            </Alert>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleBulkDelete}
              loading={deleteArticle.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
