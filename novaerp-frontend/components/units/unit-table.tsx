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
import { CreateUnitDialog } from "@/components/units/create-unit-dialog";
import { EditUnitDialog } from "@/components/units/edit-unit-dialog";
import { useDeleteUnit, useUnits } from "@/hooks/use-units";
import { getApiErrorMessage } from "@/lib/api-error";
import type { UnitResponse } from "@/types/models";

export function UnitTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingUnit, setEditingUnit] = useState<UnitResponse | null>(null);

  const { data, isPending } = useUnits(currentPage - 1, pageSize);
  const deleteUnit = useDeleteUnit();

  const handleBulkDelete = async () => {
    setDeleteError(null);
    try {
      for (const id of selectedIds) {
        await deleteUnit.mutateAsync(Number(id));
      }
      setSelectedIds([]);
      setDeleteDialogOpen(false);
    } catch (err) {
      setDeleteError(getApiErrorMessage(err, "Impossible de supprimer l'unité."));
    }
  };

  const columns: TableColumn<UnitResponse>[] = [
    {
      key: "name",
      label: "Nom",
      render: (unit) => <span className="font-medium">{unit.name}</span>,
    },
    {
      key: "symbol",
      label: "Symbole",
      render: (unit) => <Badge variant="outline">{unit.symbol}</Badge>,
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["name", "symbol"]}
        searchPlaceholder="Rechercher une unité par nom ou symbole..."
        emptyMessage={isPending ? "Chargement..." : "Aucune unité trouvée."}
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total unités</span>
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
              Nouvelle unité
            </Button>
          </>
        }
        actions={(unit) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem onClick={() => setEditingUnit(unit)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([unit.id]);
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

      <CreateUnitDialog open={createDialogOpen} onOpenChange={setCreateDialogOpen} />

      <EditUnitDialog
        unit={editingUnit}
        open={editingUnit !== null}
        onOpenChange={(open) => {
          if (!open) setEditingUnit(null);
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
              Supprimer {selectedIds.length} unité
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les unités sélectionnées seront
              définitivement supprimées.
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
              loading={deleteUnit.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
