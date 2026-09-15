"use client";

import type React from "react";
import { useParams } from "next/navigation";
import { StockTransferDetails } from "@/components/stock-transfers/stock-transfer-details";

export default function StockTransferDetailPage(): React.ReactElement {
  const params = useParams();
  const id = Number(params?.id);

  return (
    <div className="flex flex-col gap-4">
      <StockTransferDetails id={id} />
    </div>
  );
}
