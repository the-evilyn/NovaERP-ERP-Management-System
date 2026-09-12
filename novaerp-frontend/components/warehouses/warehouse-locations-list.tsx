"use client";

import {
  Add01Icon,
  AlertCircleIcon,
  Cancel01Icon,
  CheckmarkCircle02Icon,
  MoreVerticalIcon,
  PencilEdit01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Menu,
  MenuItem,
  MenuPopup,
  MenuTrigger,
} from "@/components/ui/menu";
import { Spinner } from "@/components/ui/spinner";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { CreateLocationDialog } from "@/components/warehouses/create-location-dialog";
import { EditLocationDialog } from "@/components/warehouses/edit-location-dialog";
import {
  useToggleLocationActive,
  useWarehouseLocations,
} from "@/hooks/use-warehouses";
import { getApiErrorMessage } from "@/lib/api-error";
import type {
  WarehouseLocationResponse,
  WarehouseResponse,
} from "@/types/models";

export interface WarehouseLocationsListProps {
  warehouse: WarehouseResponse;
  isAdmin?: boolean;
}

export function WarehouseLocationsList({
  warehouse,
  isAdmin = false,
}: WarehouseLocationsListProps): React.ReactElement {
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingLocation, setEditingLocation] =
    useState<WarehouseLocationResponse | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, isPending, isError, error, refetch } = useWarehouseLocations(
    warehouse.id,
    0,
    100,
  );
  const toggleLocationActive = useToggleLocationActive(warehouse.id);

  const locations = data?.content ?? [];

  const handleToggleActive = async (location: WarehouseLocationResponse) => {
    setActionError(null);
    try {
      await toggleLocationActive.mutateAsync({
        locationId: location.id,
        active: !location.active,
      });
    } catch (err) {
      setActionError(
        getApiErrorMessage(
          err,
          `Impossible de ${location.active ? "désactiver" : "activer"} l'emplacement.`,
        ),
      );
    }
  };

  return (
    <div className="flex flex-col gap-3 border-t bg-muted/20 p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h4 className="font-semibold text-sm">
              Emplacements de stockage
            </h4>
            <Badge variant="outline" size="sm">
              {locations.length}
            </Badge>
          </div>
          <p className="text-muted-foreground text-xs">
            Emplacements configurés pour l&apos;entrepôt {warehouse.name} ({warehouse.code})
          </p>
        </div>

        {isAdmin && (
          <Button
            type="button"
            size="sm"
            variant="outline"
            className="h-8 gap-1 text-xs self-start sm:self-auto"
            onClick={() => setCreateDialogOpen(true)}
            disabled={!warehouse.active}
            title={
              !warehouse.active
                ? "Impossible d'ajouter un emplacement dans un entrepôt inactif"
                : undefined
            }
          >
            <HugeiconsIcon icon={Add01Icon} className="size-3.5" />
            Nouvel emplacement
          </Button>
        )}
      </div>

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
                "Erreur lors du chargement des emplacements.",
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

      <div className="rounded-md border bg-background overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/40 hover:bg-muted/40">
              <TableHead className="w-36 ps-3">Code</TableHead>
              <TableHead className="w-48">Nom</TableHead>
              <TableHead>Description</TableHead>
              <TableHead className="w-28 text-center">Statut</TableHead>
              <TableHead className="w-28 text-center">Défaut</TableHead>
              {isAdmin && (
                <TableHead className="w-20 pe-3 text-right">Actions</TableHead>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isPending ? (
              <TableRow>
                <TableCell
                  colSpan={isAdmin ? 6 : 5}
                  className="h-20 text-center text-muted-foreground text-xs"
                >
                  <div className="flex items-center justify-center gap-2">
                    <Spinner className="size-4" />
                    <span>Chargement des emplacements...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : locations.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={isAdmin ? 6 : 5}
                  className="h-20 text-center text-muted-foreground text-xs"
                >
                  <div className="flex flex-col items-center justify-center gap-1">
                    <span>Aucun emplacement configuré pour cet entrepôt.</span>
                    {isAdmin && warehouse.active && (
                      <Button
                        type="button"
                        variant="link"
                        size="sm"
                        className="h-auto p-0 text-xs"
                        onClick={() => setCreateDialogOpen(true)}
                      >
                        Créer le premier emplacement
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ) : (
              locations.map((location) => (
                <TableRow key={location.id} className="text-xs">
                  <TableCell className="ps-3 font-mono font-medium text-primary">
                    {location.code}
                  </TableCell>
                  <TableCell className="font-medium">
                    {location.name}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {location.description ?? "—"}
                  </TableCell>
                  <TableCell className="text-center">
                    {location.active ? (
                      <Badge variant="success" size="sm">
                        Actif
                      </Badge>
                    ) : (
                      <Badge
                        variant="outline"
                        size="sm"
                        className="text-muted-foreground"
                      >
                        Inactif
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-center">
                    {location.isDefault ? (
                      <Badge variant="secondary" size="sm">
                        Par défaut
                      </Badge>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  {isAdmin && (
                    <TableCell className="pe-3 text-right">
                      <Menu>
                        <MenuTrigger
                          render={
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              className="size-7"
                            />
                          }
                        >
                          <span className="sr-only">Ouvrir le menu</span>
                          <HugeiconsIcon
                            icon={MoreVerticalIcon}
                            strokeWidth={2}
                            className="size-3.5"
                          />
                        </MenuTrigger>
                        <MenuPopup align="end">
                          <MenuItem
                            onClick={() => setEditingLocation(location)}
                          >
                            <HugeiconsIcon
                              icon={PencilEdit01Icon}
                              strokeWidth={2}
                            />
                            Modifier
                          </MenuItem>
                          <MenuItem
                            onClick={() => handleToggleActive(location)}
                            disabled={toggleLocationActive.isPending}
                          >
                            <HugeiconsIcon
                              icon={
                                location.active
                                  ? Cancel01Icon
                                  : CheckmarkCircle02Icon
                              }
                              strokeWidth={2}
                            />
                            {location.active ? "Désactiver" : "Activer"}
                          </MenuItem>
                        </MenuPopup>
                      </Menu>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <CreateLocationDialog
        warehouseId={warehouse.id}
        warehouseName={warehouse.name}
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />

      <EditLocationDialog
        warehouseId={warehouse.id}
        location={editingLocation}
        open={editingLocation !== null}
        onOpenChange={(open) => {
          if (!open) setEditingLocation(null);
        }}
      />
    </div>
  );
}
