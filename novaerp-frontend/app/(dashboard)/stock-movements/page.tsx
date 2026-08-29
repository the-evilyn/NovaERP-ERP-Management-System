"use client";

import type React from "react";
import { useState } from "react";
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from "@/components/ui/empty";
import { ArticlePicker } from "@/components/stock-movements/article-picker";
import { MovementHistory } from "@/components/stock-movements/movement-history";
import { RecordMovementForm } from "@/components/stock-movements/record-movement-form";
import { useArticle } from "@/hooks/use-articles";

export default function StockMovementsPage(): React.ReactElement {
  const [articleId, setArticleId] = useState<number | null>(null);
  const { data: article } = useArticle(articleId ?? Number.NaN);

  return (
    <div className="flex flex-col gap-4">
      <ArticlePicker articleId={articleId} onArticleChange={setArticleId} />

      {article ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="lg:col-span-1">
            <RecordMovementForm article={article} />
          </div>
          <div className="lg:col-span-2">
            <MovementHistory article={article} />
          </div>
        </div>
      ) : (
        <Empty className="rounded-2xl border">
          <EmptyHeader>
            <EmptyTitle>Aucun article sélectionné</EmptyTitle>
            <EmptyDescription>
              Choisissez un article ci-dessus pour consulter son historique de
              mouvements et en enregistrer de nouveaux.
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
    </div>
  );
}
