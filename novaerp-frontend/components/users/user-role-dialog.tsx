"use client";

import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogFooter,
  DialogHeader,
  DialogPanel,
  DialogPopup,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useUpdateUserRole } from "@/hooks/use-users";
import { getApiErrorMessage } from "@/lib/api-error";
import type { UserListItem } from "@/services/users.service";

interface UserRoleDialogProps {
  user: UserListItem | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UserRoleDialog({
  user,
  open,
  onOpenChange,
}: UserRoleDialogProps): React.ReactElement {
  const [role, setRole] = useState<"ADMIN" | "USER">("USER");
  const [error, setError] = useState<string | null>(null);
  const [loadedUserId, setLoadedUserId] = useState<number | null>(null);
  const updateRole = useUpdateUserRole();

  if (user && loadedUserId !== user.id) {
    setLoadedUserId(user.id);
    setRole(user.role);
    setError(null);
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setError(null);

    try {
      await updateRole.mutateAsync({ id: user.id, role });
      onOpenChange(false);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de modifier le rôle"));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Modifier le rôle de l’utilisateur</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <DialogPanel className="space-y-4">
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="rounded-lg border bg-muted/30 p-3 text-sm">
              <p className="font-medium text-foreground">{user?.fullName}</p>
              <p className="text-xs text-muted-foreground">{user?.email}</p>
            </div>

            <Field>
              <FieldLabel>Nouveau rôle</FieldLabel>
              <Select
                value={role}
                onValueChange={(val) => {
                  if (val === "ADMIN" || val === "USER") setRole(val);
                }}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="USER">USER (Opérateur)</SelectItem>
                  <SelectItem value="ADMIN">ADMIN (Administrateur)</SelectItem>
                </SelectContent>
              </Select>
            </Field>
          </DialogPanel>

          <DialogFooter className="mt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={updateRole.isPending}
            >
              Annuler
            </Button>
            <Button type="submit" disabled={updateRole.isPending}>
              {updateRole.isPending ? "Modification..." : "Enregistrer"}
            </Button>
          </DialogFooter>
        </form>
      </DialogPopup>
    </Dialog>
  );
}
