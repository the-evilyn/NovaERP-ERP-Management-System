"use client";

import type React from "react";
import { useMemo } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Field, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectItem,
  SelectPopup,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useArticles } from "@/hooks/use-articles";

interface ArticlePickerProps {
  articleId: number | null;
  onArticleChange: (id: number | null) => void;
}

export function ArticlePicker({
  articleId,
  onArticleChange,
}: ArticlePickerProps): React.ReactElement {
  const { data: articlesPage, isPending } = useArticles(0, 100);
  const articles = articlesPage?.content;

  const items = useMemo(
    () =>
      (articles ?? []).map((article) => ({
        label: `${article.designation} (${article.reference})`,
        value: String(article.id),
      })),
    [articles],
  );

  return (
    <Card>
      <CardHeader>
        <h2 className="font-semibold text-sm">Sélectionner un article</h2>
        <p className="text-muted-foreground text-xs">
          Choisissez un article pour consulter son historique de mouvements
          et en enregistrer de nouveaux.
        </p>
      </CardHeader>
      <CardContent>
        <Field>
          <FieldLabel htmlFor="article-picker">Article</FieldLabel>
          <Select
            items={items}
            value={articleId ? String(articleId) : null}
            onValueChange={(value) =>
              onArticleChange(value ? Number(value) : null)
            }
          >
            <SelectTrigger id="article-picker">
              <SelectValue
                placeholder={
                  isPending
                    ? "Chargement des articles..."
                    : "Rechercher un article..."
                }
              />
            </SelectTrigger>
            <SelectPopup>
              {items.map((item) => (
                <SelectItem key={item.value} value={item.value}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectPopup>
          </Select>
        </Field>
      </CardContent>
    </Card>
  );
}
