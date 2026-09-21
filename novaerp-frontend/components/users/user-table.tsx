"use client";

import {
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  PencilEdit01Icon,
  Shield01Icon,
  User02Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import PaginationTable from "@/components/shared/pagination-table";
import { UserRoleDialog } from "@/components/users/user-role-dialog";
import { UserStatusDialog } from "@/components/users/user-status-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useUsers } from "@/hooks/use-users";
import { useAuth } from "@/providers/auth-provider";
import type { UserListItem } from "@/services/users.service";

export function UserTable(): React.ReactElement {
  const { user: currentUser } = useAuth();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, isError, error } = useUsers(page - 1, pageSize);

  const [selectedUserForRole, setSelectedUserForRole] =
    useState<UserListItem | null>(null);
  const [isRoleDialogOpen, setIsRoleDialogOpen] = useState(false);

  const [selectedUserForStatus, setSelectedUserForStatus] =
    useState<UserListItem | null>(null);
  const [isStatusDialogOpen, setIsStatusDialogOpen] = useState(false);

  const handleOpenRole = (targetUser: UserListItem) => {
    setSelectedUserForRole(targetUser);
    setIsRoleDialogOpen(true);
  };

  const handleOpenStatus = (targetUser: UserListItem) => {
    setSelectedUserForStatus(targetUser);
    setIsStatusDialogOpen(true);
  };

  return (
    <div className="space-y-4">
      {isError && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
          <AlertDescription>
            {error instanceof Error
              ? error.message
              : "Erreur lors du chargement des utilisateurs"}
          </AlertDescription>
        </Alert>
      )}

      <div className="rounded-xl border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40 hover:bg-muted/40">
                <TableHead className="w-[220px]">Nom complet</TableHead>
                <TableHead className="w-[260px]">Email</TableHead>
                <TableHead className="w-[140px]">Rôle</TableHead>
                <TableHead className="w-[120px]">Statut</TableHead>
                <TableHead className="w-[160px]">Date d’inscription</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-48 text-center">
                    <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground">
                      <Spinner className="size-6 text-primary" />
                      <span className="text-sm">Chargement des utilisateurs...</span>
                    </div>
                  </TableCell>
                </TableRow>
              ) : !data || data.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-48 text-center">
                    <div className="flex flex-col items-center justify-center gap-2 text-muted-foreground">
                      <HugeiconsIcon icon={User02Icon} className="size-8" />
                      <span className="text-sm">Aucun utilisateur trouvé</span>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                data.content.map((u) => {
                  const isSelf =
                    currentUser?.id === u.id || currentUser?.email === u.email;

                  return (
                    <TableRow key={u.id} className="hover:bg-muted/20">
                      <TableCell className="font-medium">
                        <div className="flex items-center gap-2">
                          <span>{u.fullName}</span>
                          {isSelf && (
                            <Badge variant="outline" className="text-[10px] py-0 px-1.5 font-normal">
                              Vous
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm font-mono">
                        {u.email}
                      </TableCell>
                      <TableCell>
                        {u.role === "ADMIN" ? (
                          <Badge className="bg-primary/15 text-primary border-primary/25 hover:bg-primary/20 gap-1 font-medium">
                            <HugeiconsIcon icon={Shield01Icon} className="size-3" />
                            ADMIN
                          </Badge>
                        ) : (
                          <Badge variant="secondary" className="gap-1 font-medium text-muted-foreground">
                            <HugeiconsIcon icon={User02Icon} className="size-3" />
                            USER
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell>
                        {u.enabled ? (
                          <Badge
                            variant="outline"
                            className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/25 gap-1 font-normal"
                          >
                            <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-3" />
                            Actif
                          </Badge>
                        ) : (
                          <Badge
                            variant="outline"
                            className="bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/25 gap-1 font-normal"
                          >
                            <HugeiconsIcon icon={AlertCircleIcon} className="size-3" />
                            Inactif
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground text-xs">
                        {u.createdAt
                          ? new Date(u.createdAt).toLocaleDateString("fr-FR", {
                              day: "2-digit",
                              month: "short",
                              year: "numeric",
                            })
                          : "—"}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="outline"
                            size="sm"
                            className="h-8 text-xs gap-1"
                            onClick={() => handleOpenRole(u)}
                            disabled={isSelf}
                            title={isSelf ? "Vous ne pouvez pas modifier votre propre rôle" : "Modifier le rôle"}
                          >
                            <HugeiconsIcon icon={PencilEdit01Icon} className="size-3.5" />
                            Rôle
                          </Button>

                          <Button
                            variant={u.enabled ? "outline" : "default"}
                            size="sm"
                            className="h-8 text-xs"
                            onClick={() => handleOpenStatus(u)}
                            disabled={isSelf}
                            title={isSelf ? "Vous ne pouvez pas désactiver votre propre compte" : u.enabled ? "Désactiver" : "Activer"}
                          >
                            {u.enabled ? "Désactiver" : "Activer"}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>

        {data && data.totalPages > 1 && (
          <div className="border-t p-3">
            <PaginationTable
              currentPage={page}
              totalPages={data.totalPages}
              pageSize={pageSize}
              totalItems={data.totalElements}
              onPageChange={setPage}
              onPageSizeChange={setPageSize}
            />
          </div>
        )}
      </div>

      <UserRoleDialog
        user={selectedUserForRole}
        open={isRoleDialogOpen}
        onOpenChange={setIsRoleDialogOpen}
      />

      <UserStatusDialog
        user={selectedUserForStatus}
        open={isStatusDialogOpen}
        onOpenChange={setIsStatusDialogOpen}
      />
    </div>
  );
}
