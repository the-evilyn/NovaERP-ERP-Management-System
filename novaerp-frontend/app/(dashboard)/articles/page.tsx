import type React from "react";
import { ArticleTable } from "@/components/articles/article-table";

export default function ArticlesPage(): React.ReactElement {
  return (
    <div className="flex flex-col gap-4">
      <ArticleTable />
    </div>
  );
}
