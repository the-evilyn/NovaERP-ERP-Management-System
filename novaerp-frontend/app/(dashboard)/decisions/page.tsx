import type React from "react";
import { DecisionTable } from "@/components/decision/decision-table";

export default function DecisionSupportPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <DecisionTable />
    </div>
  );
}
