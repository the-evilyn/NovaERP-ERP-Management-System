"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import type React from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useSaleOrders } from "@/hooks/use-sales";
import { formatCurrency } from "@/lib/formatters";
import { CreateSalesOrderDialog } from "@/components/sales/create-sales-order-dialog";
import { SalesOrderTable } from "@/components/sales/sales-order-table";

export default function SalesPage(): React.ReactElement {
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const { data } = useSaleOrders(0, 100);
  const orders = data?.content ?? [];

  // KPIs
  const totalOrders = data?.totalElements ?? orders.length;
  const draftCount = orders.filter((o) => o.status === "DRAFT").length;
  const confirmedCount = orders.filter((o) => o.status === "CONFIRMED").length;
  const totalRevenueTtc = orders
    .filter((o) => o.status === "CONFIRMED" || o.status === "DELIVERED")
    .reduce((acc, o) => acc + (o.totalTtc || 0), 0);

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Commandes de Ventes</h1>
          <p className="text-sm text-muted-foreground">
            Gestion du cycle commercial, validation client et décrémentation automatique des stocks.
          </p>
        </div>

        <Button
          onClick={() => setCreateDialogOpen(true)}
          className="gap-2 self-start sm:self-auto"
        >
          <HugeiconsIcon icon={Add01Icon} className="size-4" />
          Créer une commande
        </Button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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
            Confirmées & Sorties de Stock
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-emerald-600">{confirmedCount}</span>
            <span className="text-xs text-emerald-700">Stock décrémenté</span>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between">
          <span className="text-xs text-muted-foreground font-medium">
            CA Confirmé (TTC)
          </span>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-primary">
              {formatCurrency(totalRevenueTtc)}
            </span>
            <span className="text-xs text-muted-foreground">MAD TTC</span>
          </div>
        </Card>
      </div>

      {/* Main Orders Table */}
      <SalesOrderTable />

      {/* Create Order Modal */}
      <CreateSalesOrderDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
      />
    </div>
  );
}
