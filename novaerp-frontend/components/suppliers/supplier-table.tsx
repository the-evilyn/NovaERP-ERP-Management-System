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
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import { CreateSupplierDialog } from "@/components/suppliers/create-supplier-dialog";
import { EditSupplierDialog } from "@/components/suppliers/edit-supplier-dialog";
import { useDeleteSupplier, useSuppliers } from "@/hooks/use-suppliers";
import { getApiErrorMessage } from "@/lib/api-error";
import type { SupplierResponse } from "@/types/models";

function initials(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function SupplierTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] =
    useState<SupplierResponse | null>(null);

  const { data, isPending } = useSuppliers(currentPage - 1, pageSize);
  const deleteSupplier = useDeleteSupplier();

  const handleBulkDelete = async () => {
    setDeleteError(null);
    try {
      for (const id of selectedIds) {
        await deleteSupplier.mutateAsync(Number(id));
      }
      setSelectedIds([]);
      setDeleteDialogOpen(false);
    } catch (err) {
      setDeleteError(
        getApiErrorMessage(err, "Impossible de supprimer le fournisseur."),
      );
    }
  };

  const columns: TableColumn<SupplierResponse>[] = [
    {
      key: "supplier",
      label: "Fournisseur",
      render: (supplier) => (
        <div className="flex max-w-80 items-center gap-3">
          <Avatar className="size-10 rounded-md">
            <AvatarFallback className="rounded-md">
              {initials(supplier.name)}
            </AvatarFallback>
          </Avatar>
          <div className="flex w-full flex-col">
            <span className="truncate font-medium">{supplier.name}</span>
            <span className="truncate text-muted-foreground text-xs">
              Fournisseur #{supplier.id}
            </span>
          </div>
        </div>
      ),
    },
    {
      key: "email",
      label: "Email",
      render: (supplier) => (
        <span className="text-muted-foreground">{supplier.email ?? "—"}</span>
      ),
    },
    {
      key: "phone",
      label: "Téléphone",
      render: (supplier) => (
        <span className="text-muted-foreground">{supplier.phone ?? "—"}</span>
      ),
    },
    {
      key: "address",
      label: "Adresse",
      render: (supplier) => (
        <span className="text-muted-foreground">
          {supplier.address ?? "—"}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["name", "email", "phone"]}
        searchPlaceholder="Rechercher un fournisseur par nom ou email..."
        emptyMessage={
          isPending ? "Chargement..." : "Aucun fournisseur trouvé."
        }
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total fournisseurs</span>
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
              Nouveau fournisseur
            </Button>
          </>
        }
        actions={(supplier) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem onClick={() => setEditingSupplier(supplier)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([supplier.id]);
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

      <CreateSupplierDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />

      <EditSupplierDialog
        supplier={editingSupplier}
        open={editingSupplier !== null}
        onOpenChange={(open) => {
          if (!open) setEditingSupplier(null);
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
              Supprimer {selectedIds.length} fournisseur
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les fournisseurs sélectionnés
              seront définitivement supprimés.
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
              loading={deleteSupplier.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
