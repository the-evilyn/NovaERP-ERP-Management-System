"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { usePurchaseOrders } from "@/hooks/use-purchases";
import { formatCurrency } from "@/lib/formatters";
import { CreatePurchaseOrderDialog } from "@/components/purchases/create-purchase-order-dialog";
import { PurchaseOrderTable } from "@/components/purchases/purchase-order-table";

export default function PurchasesPage(): React.ReactElement {
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const { data } = usePurchaseOrders(0, 100);
  const orders = data?.content ?? [];

  // KPIs
  const totalOrders = data?.totalElements ?? orders.length;
  const draftCount = orders.filter((o) => o.status === "DRAFT").length;
  const confirmedCount = orders.filter((o) => o.status === "CONFIRMED").length;
  const receivedCount = orders.filter((o) => o.status === "RECEIVED").length;
  const totalSpentTtc = orders
    .filter((o) => o.status === "CONFIRMED" || o.status === "RECEIVED")
    .reduce((acc, o) => acc + (o.totalTtc || 0), 0);

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Commandes Fournisseurs (Achats)</h1>
          <p className="text-sm text-muted-foreground">
            Gestion du cycle d&apos;approvisionnement, suivi des commandes d&apos;achat et réception en stock (IN).
          </p>
        </div>

        <Button
          onClick={() => setCreateDialogOpen(true)}
          className="gap-2 self-start sm:self-auto"
        >
          <HugeiconsIcon icon={Add01Icon} className="size-4" />
          Nouvelle commande d&apos;achat
        </Button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            Total Commandes
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold">{totalOrders}</span>
            <span className="text-xs text-muted-foreground">Actives</span>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            Brouillons (En attente)
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-amber-600">{draftCount}</span>
            <span className="text-xs text-muted-foreground">À confirmer</span>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            Confirmées (En transit)
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-sky-600">{confirmedCount}</span>
            <span className="text-xs text-sky-700">À réceptionner</span>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            Réceptionnées
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-emerald-600">{receivedCount}</span>
            <span className="text-xs text-emerald-700">Stock incrémenté</span>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            Engagé Fournisseurs
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-xl font-bold text-primary">{formatCurrency(totalSpentTtc)}</span>
            <span className="text-xs text-muted-foreground">MAD TTC</span>
          </div>
        </Card>
      </div>

      {/* Main Orders Table */}
      <PurchaseOrderTable />

      {/* Create Order Modal */}
      <CreatePurchaseOrderDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />
    </div>
  );
}
