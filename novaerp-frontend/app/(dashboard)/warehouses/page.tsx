import type React from "react";
import { WarehouseTable } from "@/components/warehouses/warehouse-table";

export default function WarehousesPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-bold text-2xl tracking-tight">
            Warehouses / Entrepôts
          </h1>
          <p className="text-muted-foreground text-sm">
            Gestion des entrepôts de stockage et de leurs emplacements.
          </p>
        </div>
      </div>
      <WarehouseTable />
    </div>
  );
}
