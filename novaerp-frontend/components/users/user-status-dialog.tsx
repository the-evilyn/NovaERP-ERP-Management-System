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
import { useUpdateUserStatus } from "@/hooks/use-users";
import { getApiErrorMessage } from "@/lib/api-error";
import type { UserListItem } from "@/services/users.service";

interface UserStatusDialogProps {
  user: UserListItem | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UserStatusDialog({
  user,
  open,
  onOpenChange,
}: UserStatusDialogProps): React.ReactElement {
  const [error, setError] = useState<string | null>(null);
  const updateStatus = useUpdateUserStatus();

  if (!user) return <></>;

  const nextStatus = !user.enabled;
  const actionLabel = nextStatus ? "Activer" : "Désactiver";

  const handleConfirm = async () => {
    setError(null);
    try {
      await updateStatus.mutateAsync({ id: user.id, enabled: nextStatus });
      onOpenChange(false);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          `Impossible de ${actionLabel.toLowerCase()} le compte utilisateur`,
        ),
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPopup className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {actionLabel} le compte utilisateur
          </DialogTitle>
        </DialogHeader>

        <DialogPanel className="space-y-4">
          {error && (
            <Alert variant="error">
              <HugeiconsIcon icon={AlertCircleIcon} className="size-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <p className="text-sm text-muted-foreground">
            {nextStatus
              ? `Êtes-vous sûr de vouloir réactiver le compte de ${user.fullName} (${user.email}) ? Cet utilisateur pourra de nouveau se connecter au système.`
              : `Êtes-vous sûr de vouloir désactiver le compte de ${user.fullName} (${user.email}) ? Cet utilisateur ne pourra plus se connecter au système.`}
          </p>
        </DialogPanel>

        <DialogFooter className="mt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={updateStatus.isPending}
          >
            Annuler
          </Button>
          <Button
            variant={nextStatus ? "default" : "destructive"}
            onClick={handleConfirm}
            disabled={updateStatus.isPending}
          >
            {updateStatus.isPending ? "Traitement..." : actionLabel}
          </Button>
        </DialogFooter>
      </DialogPopup>
    </Dialog>
  );
}
