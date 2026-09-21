import type React from "react";
import { UserTable } from "@/components/users/user-table";

export default function UsersPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-bold text-2xl tracking-tight">
            Gestion des Utilisateurs
          </h1>
          <p className="text-muted-foreground text-sm">
            Administration des comptes d’accès, des rôles applicatifs et de l’activation des utilisateurs.
          </p>
        </div>
      </div>
      <UserTable />
    </div>
  );
}
