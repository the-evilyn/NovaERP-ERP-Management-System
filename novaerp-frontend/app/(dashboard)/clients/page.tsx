import type React from "react";
import { ClientTable } from "@/components/clients/client-table";

export default function ClientsPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <ClientTable />
    </div>
  );
}
