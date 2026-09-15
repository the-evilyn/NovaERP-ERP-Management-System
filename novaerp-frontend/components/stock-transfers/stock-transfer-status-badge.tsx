"use client";

import type React from "react";
import { Badge } from "@/components/ui/badge";
import type { StockTransferStatus } from "@/types/models";

interface StockTransferStatusBadgeProps {
  status: StockTransferStatus;
  className?: string;
}

export function StockTransferStatusBadge({
  status,
  className,
}: StockTransferStatusBadgeProps): React.ReactElement {
  switch (status) {
    case "DRAFT":
      return (
        <Badge
          variant="outline"
          className={`border-amber-400 bg-amber-50 text-amber-800 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-300 font-medium ${className ?? ""}`}
        >
          Brouillon
        </Badge>
      );
    case "COMPLETED":
      return (
        <Badge
          variant="outline"
          className={`border-emerald-400 bg-emerald-50 text-emerald-800 dark:border-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 font-medium ${className ?? ""}`}
        >
          Terminé
        </Badge>
      );
    case "CANCELLED":
      return (
        <Badge
          variant="outline"
          className={`border-destructive/30 bg-destructive/10 text-destructive dark:border-destructive/40 dark:bg-destructive/20 font-medium ${className ?? ""}`}
        >
          Annulé
        </Badge>
      );
    default:
      return (
        <Badge variant="outline" className={className}>
          {status}
        </Badge>
      );
  }
}
