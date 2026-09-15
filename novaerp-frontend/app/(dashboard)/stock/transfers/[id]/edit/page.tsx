"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon, ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import type React from "react";
import { StockTransferForm } from "@/components/stock-transfers/stock-transfer-form";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { useStockTransfer } from "@/hooks/use-stock-transfers";

export default function EditStockTransferPage(): React.ReactElement {
  const router = useRouter();
  const params = useParams();
  const id = Number(params?.id);

  const { data: transfer, isLoading, isError } = useStockTransfer(id);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="flex items-center gap-2 text-muted-foreground text-sm">
          <Spinner className="size-5" />
          Chargement du transfert...
        </div>
      </div>
    );
  }

  if (isError || !transfer) {
    return (
      <div className="space-y-4">
        <Button
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => router.push("/stock/transfers")}
        >
          <HugeiconsIcon icon={ArrowLeft01Icon} className="size-4" />
          Retour aux transferts
        </Button>
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
          <AlertDescription>
            Impossible de charger le transfert de stock à modifier.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (transfer.status !== "DRAFT") {
    return (
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => router.push(`/stock/transfers/${id}`)}
          >
            <HugeiconsIcon icon={ArrowLeft01Icon} className="size-4" />
          </Button>
          <h1 className="font-bold text-xl tracking-tight">
            Modification non autorisée
          </h1>
        </div>
        <Alert variant="warning">
          <HugeiconsIcon icon={AlertCircleIcon} className="size-4 shrink-0" />
          <AlertDescription className="text-xs">
            Seuls les transferts en statut <span className="font-semibold">Brouillon (DRAFT)</span> peuvent être modifiés.
            Le transfert {transfer.transferNumber} est actuellement en statut{" "}
            <span className="font-semibold">{transfer.status}</span>.
          </AlertDescription>
        </Alert>
        <div>
          <Button
            variant="outline"
            size="sm"
            render={<Link href={`/stock/transfers/${id}`} />}
          >
            Voir les détails du transfert
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <StockTransferForm initialTransfer={transfer} isEdit={true} />
    </div>
  );
}
