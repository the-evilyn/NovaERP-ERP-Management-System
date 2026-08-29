import type React from "react";
import { CategoryTable } from "@/components/categories/category-table";

export default function CategoriesPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <CategoryTable />
    </div>
  );
}
