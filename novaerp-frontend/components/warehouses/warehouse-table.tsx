"use client";

import {
  Add01Icon,
  AlertCircleIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  LocationAdd01Icon,
  MoreVerticalIcon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import { DataTable, type TableColumn } from "@/components/shared/data-table";
import PaginationTable from "@/components/shared/pagination-table";
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
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CreateLocationDialog } from "@/components/warehouses/create-location-dialog";
import { CreateWarehouseDialog } from "@/components/warehouses/create-warehouse-dialog";
import { EditWarehouseDialog } from "@/components/warehouses/edit-warehouse-dialog";
import { WarehouseLocationsList } from "@/components/warehouses/warehouse-locations-list";
import { WarehouseStockList } from "@/components/warehouses/warehouse-stock-list";
import {
  useToggleWarehouseActive,
  useWarehouses,
} from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import { useAuth } from "@/providers/auth-provider";
import type { WarehouseResponse } from "@/types/models";

export interface WarehouseTableProps {
  initialActiveFilter?: boolean;
}

interface WarehouseExpandedRowProps {
  warehouse: WarehouseResponse;
  isAdmin: boolean;
}

function WarehouseExpandedRow({
  warehouse,
  isAdmin,
}: WarehouseExpandedRowProps): React.ReactElement {
  const [activeTab, setActiveTab] = useState<"locations" | "stock">(
    "locations",
  );

  return (
    <div className="flex flex-col bg-muted/10">
      <div className="bg-background/40 px-4 pt-2">
        <Tabs
          value={activeTab}
          onValueChange={(val) => {
            if (val === "locations" || val === "stock") {
              setActiveTab(val);
            }
          }}
          className="w-full gap-0"
        >
          <TabsList variant="underline">
            <TabsTrigger value="locations" className="text-xs sm:text-xs">
              Emplacements
            </TabsTrigger>
            <TabsTrigger value="stock" className="text-xs sm:text-xs">
              Stock
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <div>
        {activeTab === "locations" ? (
          <WarehouseLocationsList warehouse={warehouse} isAdmin={isAdmin} />
        ) : (
          <WarehouseStockList warehouseId={warehouse.id} />
        )}
      </div>
    </div>
  );
}

export function WarehouseTable({
  initialActiveFilter,
}: WarehouseTableProps = {}): React.ReactElement {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [activeFilter, setActiveFilter] = useState<boolean | undefined>(
    initialActiveFilter,
  );
  const [search, setSearch] = useState("");

  const [createWarehouseOpen, setCreateWarehouseOpen] = useState(false);
  const [editingWarehouse, setEditingWarehouse] =
    useState<WarehouseResponse | null>(null);
  const [createLocationWarehouse, setCreateLocationWarehouse] =
    useState<WarehouseResponse | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isPending, isError, error, refetch } = useWarehouses(
    currentPage - 1,
    pageSize,
    activeFilter,
  );
  const toggleWarehouseActive = useToggleWarehouseActive();

  const handleToggleActive = async (warehouse: WarehouseResponse) => {
    setActionError(null);
    try {
      await toggleWarehouseActive.mutateAsync({
        id: warehouse.id,
        active: !warehouse.active,
      });
    } catch (err) {
      setActionError(
        getApiErrorMessage(
          err,
          `Impossible de ${warehouse.active ? "désactiver" : "activer"} l'entrepôt.`,
        ),
      );
    }
  };

  const handleActiveFilterChange = (filter: boolean | undefined) => {
    setActiveFilter(filter);
    setCurrentPage(1);
  };

  const columns: TableColumn<WarehouseResponse>[] = [
    {
      key: "code",
      label: "Code",
      render: (warehouse) => (
        <span className="font-mono font-semibold text-primary">
          {warehouse.code}
        </span>
      ),
    },
    {
      key: "name",
      label: "Nom",
      render: (warehouse) => (
        <div className="flex flex-col">
          <span className="font-medium">{warehouse.name}</span>
          {warehouse.description && (
            <span className="max-w-96 truncate text-muted-foreground text-xs">
              {warehouse.description}
            </span>
          )}
        </div>
      ),
    },
    {
      key: "address",
      label: "Adresse",
      render: (warehouse) => (
        <span className="max-w-72 truncate text-muted-foreground text-xs">
          {warehouse.address ?? "—"}
        </span>
      ),
    },
    {
      key: "active",
      label: "Statut",
      render: (warehouse) =>
        warehouse.active ? (
          <Badge variant="success">Actif</Badge>
        ) : (
          <Badge variant="outline" className="text-muted-foreground">
            Inactif
          </Badge>
        ),
    },
    {
      key: "isDefault",
      label: "Défaut",
      render: (warehouse) =>
        warehouse.isDefault ? (
          <Badge variant="secondary">Par défaut</Badge>
        ) : (
          <span className="text-muted-foreground text-xs">—</span>
        ),
    },
  ];

  return (
    <div className="space-y-4">
      {actionError && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} />
          <AlertDescription className="flex items-center justify-between gap-2">
            <span>{actionError}</span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-6 px-2 text-xs"
              onClick={() => setActionError(null)}
            >
              Fermer
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {isError && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} />
          <AlertDescription className="flex items-center justify-between gap-2">
            <span>
              {getApiErrorMessage(
                error,
                "Erreur lors de la récupération des entrepôts.",
              )}
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-6 px-2 text-xs"
              onClick={() => refetch()}
            >
              Réessayer
            </Button>
          </AlertDescription>
        </Alert>
      )}

      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["code", "name", "address", "description"]}
        searchPlaceholder="Rechercher un entrepôt par nom ou code..."
        emptyMessage={
          isPending
            ? "Chargement des entrepôts..."
            : activeFilter !== undefined
              ? "Aucun entrepôt ne correspond au filtre de statut sélectionné."
              : "Aucun entrepôt trouvé."
        }
        searchValue={search}
        onSearchChange={setSearch}
        expandable
        renderExpandedRow={(warehouse) => (
          <WarehouseExpandedRow warehouse={warehouse} isAdmin={isAdmin} />
        )}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total entrepôts</span>
              <span className="font-semibold">{data.totalElements}</span>
            </div>
          ) : undefined
        }
        customHeader={
          <div className="flex flex-wrap items-center gap-2">
            {/* Active Status Filter: All / Active / Inactive */}
            <div className="flex items-center gap-1 rounded-lg bg-muted/40 p-1 text-xs">
              <Button
                type="button"
                variant={activeFilter === undefined ? "default" : "ghost"}
                size="sm"
                className="h-7 text-xs"
                onClick={() => handleActiveFilterChange(undefined)}
              >
                Tous
              </Button>
              <Button
                type="button"
                variant={activeFilter === true ? "default" : "ghost"}
                size="sm"
                className="h-7 text-xs"
                onClick={() => handleActiveFilterChange(true)}
              >
                Actifs
              </Button>
              <Button
                type="button"
                variant={activeFilter === false ? "default" : "ghost"}
                size="sm"
                className="h-7 text-xs"
                onClick={() => handleActiveFilterChange(false)}
              >
                Inactifs
              </Button>
            </div>

            {isAdmin && (
              <Button
                type="button"
                onClick={() => setCreateWarehouseOpen(true)}
              >
                <HugeiconsIcon icon={Add01Icon} strokeWidth={2} />
                Nouvel entrepôt
              </Button>
            )}
          </div>
        }
        actions={
          isAdmin
            ? (warehouse) => (
                <Menu>
                  <MenuTrigger
                    render={<Button variant="ghost" size="icon-sm" />}
                  >
                    <span className="sr-only">Ouvrir le menu</span>
                    <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
                  </MenuTrigger>
                  <MenuPopup align="end">
                    <MenuItem onClick={() => setEditingWarehouse(warehouse)}>
                      <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                      Modifier
                    </MenuItem>
                    <MenuItem
                      onClick={() => handleToggleActive(warehouse)}
                      disabled={toggleWarehouseActive.isPending}
                    >
                      <HugeiconsIcon
                        icon={
                          warehouse.active
                            ? Cancel01Icon
                            : CheckmarkCircle02Icon
                        }
                        strokeWidth={2}
                      />
                      {warehouse.active ? "Désactiver" : "Activer"}
                    </MenuItem>
                    <MenuSeparator />
                    <MenuItem
                      onClick={() => setCreateLocationWarehouse(warehouse)}
                      disabled={!warehouse.active}
                    >
                      <HugeiconsIcon
                        icon={LocationAdd01Icon}
                        strokeWidth={2}
                      />
                      Nouvel emplacement
                    </MenuItem>
                  </MenuPopup>
                </Menu>
              )
            : undefined
        }
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

      {/* Dialogs */}
      <CreateWarehouseDialog
        open={createWarehouseOpen}
        onOpenChange={setCreateWarehouseOpen}
      />

      <EditWarehouseDialog
        warehouse={editingWarehouse}
        open={editingWarehouse !== null}
        onOpenChange={(open) => {
          if (!open) setEditingWarehouse(null);
        }}
      />

      <CreateLocationDialog
        warehouseId={createLocationWarehouse?.id ?? null}
        warehouseName={createLocationWarehouse?.name}
        open={createLocationWarehouse !== null}
        onOpenChange={(open) => {
          if (!open) setCreateLocationWarehouse(null);
        }}
      />
    </div>
  );
}
