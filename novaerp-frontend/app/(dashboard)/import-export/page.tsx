"use client";

import type React from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ImportExportCard } from "@/components/import-export/import-export-card";
import { useAuth } from "@/providers/auth-provider";
import { Spinner } from "@/components/ui/spinner";

export default function ImportExportPage(): React.ReactElement {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && user && user.role !== "ADMIN") {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  if (isLoading || (user && user.role !== "ADMIN")) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Spinner className="size-6 text-muted-foreground" />
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <p className="text-muted-foreground text-sm">
        Exportez vos données au format CSV ou importez un fichier pour créer
        de nouvelles fiches. Un import est sûr à relancer : les lignes dont la
        référence (articles) ou le nom (catégories, fournisseurs) existe déjà
        sont ignorées, jamais écrasées.
      </p>

      <ImportExportCard
        entity="articles"
        title="Articles"
        description="Le catalogue produits : référence, désignation, prix, stock, catégorie, fournisseur principal."
        columns={[
          "reference",
          "designation",
          "brand",
          "barcode",
          "category",
          "unit",
          "purchasePriceHt",
          "unitCostTtc",
          "salePriceHt",
          "stockQuantity",
          "minStockQuantity",
          "serialTracked",
          "description",
          "notes",
          "primarySupplier",
        ]}
      />

      <ImportExportCard
        entity="categories"
        title="Catégories"
        description="La liste des catégories d'articles."
        columns={["name", "description"]}
      />

      <ImportExportCard
        entity="suppliers"
        title="Fournisseurs"
        description="La liste des fournisseurs."
        columns={["name", "email", "phone", "address"]}
      />
    </div>
  );
}
