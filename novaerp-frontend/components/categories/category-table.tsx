"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
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
import { CreateCategoryDialog } from "@/components/categories/create-category-dialog";
import { EditCategoryDialog } from "@/components/categories/edit-category-dialog";
import { useCategories, useDeleteCategory } from "@/hooks/use-categories";
import { getApiErrorMessage } from "@/lib/api-error";
import type { CategoryResponse } from "@/types/models";

const dateFormatter = new Intl.DateTimeFormat("fr-MA", {
  day: "2-digit",
  month: "long",
  year: "numeric",
});

export function CategoryTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingCategory, setEditingCategory] =
    useState<CategoryResponse | null>(null);

  const { data, isPending } = useCategories(currentPage - 1, pageSize);
  const deleteCategory = useDeleteCategory();

  const handleBulkDelete = async () => {
    setDeleteError(null);
    try {
      for (const id of selectedIds) {
        await deleteCategory.mutateAsync(Number(id));
      }
      setSelectedIds([]);
      setDeleteDialogOpen(false);
    } catch (err) {
      setDeleteError(
        getApiErrorMessage(err, "Impossible de supprimer la catégorie."),
      );
    }
  };

  const columns: TableColumn<CategoryResponse>[] = [
    {
      key: "name",
      label: "Nom",
      render: (category) => (
        <span className="font-medium">{category.name}</span>
      ),
    },
    {
      key: "description",
      label: "Description",
      render: (category) => (
        <span className="max-w-96 truncate text-muted-foreground">
          {category.description ?? "—"}
        </span>
      ),
    },
    {
      key: "createdAt",
      label: "Créée le",
      render: (category) => (
        <span className="text-muted-foreground">
          {dateFormatter.format(new Date(category.createdAt))}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["name", "description"]}
        searchPlaceholder="Rechercher une catégorie par nom..."
        emptyMessage={isPending ? "Chargement..." : "Aucune catégorie trouvée."}
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total catégories</span>
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
              Nouvelle catégorie
            </Button>
          </>
        }
        actions={(category) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem onClick={() => setEditingCategory(category)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([category.id]);
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

      <CreateCategoryDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />

      <EditCategoryDialog
        category={editingCategory}
        open={editingCategory !== null}
        onOpenChange={(open) => {
          if (!open) setEditingCategory(null);
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
              Supprimer {selectedIds.length} catégorie
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les catégories sélectionnées
              seront définitivement supprimées.
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
              loading={deleteCategory.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
