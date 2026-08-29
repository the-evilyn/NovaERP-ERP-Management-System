"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Add01Icon,
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
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import { CreateClientDialog } from "@/components/clients/create-client-dialog";
import { EditClientDialog } from "@/components/clients/edit-client-dialog";
import { parseCsv } from "@/lib/csv";
import { useClients, useCreateClients, useDeleteClients } from "@/hooks/use-clients";
import type { Client } from "@/types/models";

const dateFormatter = new Intl.DateTimeFormat("fr-MA", {
  day: "2-digit",
  month: "long",
  year: "numeric",
});

function initials(name: string): string {
  return name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

function InfoField({
  label,
  value,
}: {
  label: string;
  value: string | null;
}): React.ReactElement {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-muted-foreground text-xs">{label}</span>
      <span className="text-sm">{value ?? "—"}</span>
    </div>
  );
}

function ClientExpandedRow({ client }: { client: Client }): React.ReactElement {
  return (
    <div className="p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="font-semibold text-sm">Détails du client</h3>
        <span className="text-muted-foreground text-xs">
          Client depuis le {dateFormatter.format(new Date(client.createdAt))}
        </span>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <InfoField label="Email" value={client.email} />
        <InfoField label="Téléphone" value={client.telephone} />
        <InfoField label="Adresse" value={client.adresse} />
      </div>
    </div>
  );
}

export function ClientTable(): React.ReactElement {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState("");
  const [selectedIds, setSelectedIds] = useState<(string | number)[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isPending } = useClients(currentPage - 1, pageSize);
  const deleteClients = useDeleteClients();
  const createClients = useCreateClients();

  const handleBulkDelete = async () => {
    await deleteClients.mutateAsync(selectedIds.map(Number));
    setSelectedIds([]);
    setDeleteDialogOpen(false);
  };

  const handleImportCsv = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    const text = await file.text();
    const rows = parseCsv(text);

    const clients = rows
      .filter((row) => row.nom)
      .map((row) => ({
        nom: row.nom,
        email: row.email || null,
        telephone: row.telephone || null,
        adresse: row.adresse || null,
      }));

    if (clients.length > 0) {
      await createClients.mutateAsync(clients);
    }
  };

  const columns: TableColumn<Client>[] = [
    {
      key: "client",
      label: "Client",
      render: (client) => (
        <div className="flex max-w-80 items-center gap-3">
          <Avatar className="size-10 rounded-md">
            <AvatarFallback className="rounded-md">
              {initials(client.nom)}
            </AvatarFallback>
          </Avatar>
          <div className="flex w-full flex-col">
            <span className="truncate font-medium">{client.nom}</span>
            <span className="truncate text-muted-foreground text-xs">
              Client #{client.id}
            </span>
          </div>
        </div>
      ),
    },
    {
      key: "email",
      label: "Email",
      render: (client) => (
        <span className="text-muted-foreground">{client.email ?? "—"}</span>
      ),
    },
    {
      key: "telephone",
      label: "Téléphone",
      render: (client) => (
        <span className="text-muted-foreground">{client.telephone ?? "—"}</span>
      ),
    },
    {
      key: "adresse",
      label: "Adresse",
      render: (client) => (
        <span className="text-muted-foreground">{client.adresse ?? "—"}</span>
      ),
    },
    {
      key: "statut",
      label: "Statut",
      render: () => (
        <Badge variant="outline">
          <span
            aria-hidden="true"
            className="size-1.5 rounded-full bg-emerald-500"
          />
          Actif
        </Badge>
      ),
    },
    {
      key: "createdAt",
      label: "Créé le",
      render: (client) => (
        <span className="text-muted-foreground">
          {dateFormatter.format(new Date(client.createdAt))}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <DataTable
        data={isPending ? [] : (data?.content ?? [])}
        columns={columns}
        searchKeys={["nom", "email"]}
        searchPlaceholder="Rechercher un client par nom ou email..."
        emptyMessage={isPending ? "Chargement..." : "Aucun client trouvé."}
        searchValue={search}
        onSearchChange={setSearch}
        selectable
        selectedIds={selectedIds}
        onSelectionChange={setSelectedIds}
        expandable
        renderExpandedRow={(client) => <ClientExpandedRow client={client} />}
        footer={
          data && data.totalElements > 0 ? (
            <div className="flex items-center justify-between text-sm">
              <span>Total clients</span>
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
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={handleImportCsv}
            />
            <Button
              variant="outline"
              loading={createClients.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              <HugeiconsIcon icon={Upload01Icon} strokeWidth={2} />
              Importer CSV
            </Button>
            <Button onClick={() => setCreateDialogOpen(true)}>
              <HugeiconsIcon icon={Add01Icon} strokeWidth={2} />
              Nouveau client
            </Button>
          </>
        }
        actions={(client) => (
          <Menu>
            <MenuTrigger render={<Button variant="ghost" size="icon-sm" />}>
              <span className="sr-only">Ouvrir le menu</span>
              <HugeiconsIcon icon={MoreVerticalIcon} strokeWidth={2} />
            </MenuTrigger>
            <MenuPopup align="end">
              <MenuItem render={<Link href={`/clients/${client.id}`} />}>
                <HugeiconsIcon icon={ViewIcon} strokeWidth={2} />
                Voir la fiche
              </MenuItem>
              <MenuItem onClick={() => setEditingClient(client)}>
                <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} />
                Modifier
              </MenuItem>
              <MenuSeparator />
              <MenuItem
                variant="destructive"
                onClick={() => {
                  setSelectedIds([client.id]);
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

      <CreateClientDialog open={createDialogOpen} onOpenChange={setCreateDialogOpen} />

      <EditClientDialog
        client={editingClient}
        open={editingClient !== null}
        onOpenChange={(open) => {
          if (!open) setEditingClient(null);
        }}
      />

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Supprimer {selectedIds.length} client
              {selectedIds.length > 1 ? "s" : ""} ?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Cette action est irréversible. Les clients sélectionnés seront
              définitivement supprimés.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleBulkDelete}
              loading={deleteClients.isPending}
            >
              Supprimer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
