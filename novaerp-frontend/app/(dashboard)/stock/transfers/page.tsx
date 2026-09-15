import type React from "react";
import { StockTransferTable } from "@/components/stock-transfers/stock-transfer-table";

export default function StockTransfersPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-bold text-2xl tracking-tight">
            Transferts de stock
          </h1>
          <p className="text-muted-foreground text-sm">
            Gestion des mouvements et transferts d&apos;articles entre entrepôts et emplacements.
          </p>
        </div>
      </div>
      <StockTransferTable />
    </div>
  );
}
