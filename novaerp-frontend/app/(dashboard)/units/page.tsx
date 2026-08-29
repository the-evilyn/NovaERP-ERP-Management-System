import type React from "react";
import { UnitTable } from "@/components/units/unit-table";

export default function UnitsPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <UnitTable />
    </div>
  );
}
