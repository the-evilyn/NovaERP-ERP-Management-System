import type React from "react";
import { SupplierTable } from "@/components/suppliers/supplier-table";

export default function SuppliersPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <SupplierTable />
    </div>
  );
}
