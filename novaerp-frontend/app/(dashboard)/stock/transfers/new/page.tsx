import type React from "react";
import { StockTransferForm } from "@/components/stock-transfers/stock-transfer-form";

export default function NewStockTransferPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <StockTransferForm />
    </div>
  );
}
