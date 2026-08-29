import type React from "react";
import { ImportExportCard } from "@/components/import-export/import-export-card";

export default function ImportExportPage(): React.ReactElement {
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
